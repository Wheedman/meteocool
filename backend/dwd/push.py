#!/usr/bin/python3

# very inefficient (we can simply convert between the two coordinate systems
# and get xy directly without two O(n^2) operations). this is just a poc! XXX

import os
import sys
import json
import glob
import logging
from gobiko.apns.exceptions import BadDeviceToken
from gobiko.apns import APNsClient
from pymongo import MongoClient
from scipy.spatial import distance
import requests
import wradlib as wrl

logging.basicConfig(level=logging.WARN)

def closest_node(node, nodes):
    closest_index = distance.cdist([node], nodes).argmin()
    return closest_index

if __name__ == "__main__":
    # programm parameters
    radar_files = sys.argv[1]
    browser_notify_url = sys.argv[2]

    # Apple Push setup
    apns_config_file = '/etc/apns.json'
    apns = None
    if os.path.isfile(apns_config_file):
        config = None
        with open(apns_config_file) as conf_file:
            config = json.load(conf_file)
        apns = APNsClient(
            team_id=config["team_id"],
            bundle_id=config["bundle_id"],
            auth_key_id=config["auth_key_id"],
            auth_key_filepath=config["auth_key_filepath"],
            use_sandbox=config["use_sandbox"]
        )

    # mongodb setup
    db_client = MongoClient("mongodb://mongo:27017/")
    # both will be created automatically when the first document is inserted
    db = db_client["meteocool"]
    collection = db["collection"]

    # forecast file enumeration & import
    forecast_files = sorted(glob.glob(radar_files + "/FX*_*_MF002"))
    max_ahead = 0
    forecast_maps = {}
    for f in forecast_files:
        forecast_maps[cnt] = wrl.io.radolan.read_radolan_composite(f)
        max_ahead = cnt + 5
    logging.info("Maximum forecast in minutes: %d" % max_ahead)

    # wradlib setup
    gridsize = 900
    radolan_grid_ll = wrl.georef.get_radolan_grid(gridsize, gridsize, wgs84=True)
    linearized_grid = []
    for lon in radolan_grid_ll:
        for lat in lon:
            linearized_grid.append(lat)

    # iterate through all db entries and push browser events to the app backend,
    # ios push events to apple
    cursor = collection.find({})
    cnt = 0
    for document in cursor:
        break
        doc_id = document["_id"]
        lat = document["lat"]
        lon = document["lon"]
        token = document["token"]
        ahead = document["ahead"]
        intensity = document["intensity"]
        ios_onscreen = document["ios_onscreen"]
        source = document["ios_onscreen"]

        if ahead > 110 or ahead%5 != 0:
            logging.info("%s: invalid ahead value" % doc_id)
            break
        data = forecast_maps[ahead]

        # XXX check lat/lon against the bounds of the dwd data here
        # to avoid useless calculations here

        result = closest_node((lon, lat), linearized_grid)
        xy = (int(result / gridsize), int(result % gridsize))
        reported_intensity = data[0][xy[0]][xy[1]]
        if reported_intensity > intensity:
            logging.info("%s: intensity %d > %d matches in %d min forecast (type=%s)" % (doc_id, reported_intensity, intensity, ahead, source))
            if source == "browser":
                requests.post(browser_notify_url, json={"token": token, "ahead": ahead})
            elif source == "ios":
                if apns:
                    # https://developer.apple.com/library/archive/documentation/NetworkingInternet/
                    # Conceptual/RemoteNotificationsPG/PayloadKeyReference.html#//apple_ref/doc/uid/TP40008194-CH17-SW1
                    try:
                        apns.send_message(token, ("Rain expected in %d minutes!" % ahead), badge=0, sound="pulse.aiff")
                    except BadDeviceToken:
                        logging.info("%s: sending iOS notification failed with BadDeviceToken, removing push client", doc_id)
                        collection.remove(doc_id)
                    else:
                        logging.info("%s: sent iOS notification", doc_id)
                        # mark notification as delivered in the database, so we can
                        # clear it as soon as the rain stops.
                        db.collection.update(doc_id, {"$set": {"ios_onscreen": True}})
                else:
                    logging.warn("iOS push not configured but iOS source requested")
            else:
                logging.warn("unknown source type %s" % source)
        else:
            if ios_onscreen:
                # rain has stopped and the notification is (possibly) still
                # displayed on the device.
                # XXX ios app should notify our api as soon as it is launched (and notifications are clearead)
                # XXX so we can reset the flag and don't send this silent push.
                try:
                    apns.send_message(token, None, badge=0, content_available=True, extra={"clear_all": True})
                except BadDeviceToken:
                    logging.info("%s: silent iOS notification failed with BadDeviceToken, removing push client", doc_id)
                    collection.remove(doc_id)
                else:
                    logging.info("%s: sent silent notification" % doc_id)
                    db.collection.update(doc_id, { "$set": {"ios_onscreen": True} })
        cnt = cnt + 1

    logging.info("===> Processed %d total clients" % cnt)
