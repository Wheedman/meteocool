package com.meteocool.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import com.google.android.gms.location.LocationResult
import com.meteocool.location.LocationResultHelper.Companion.getDistanceToLastLocation
import java.util.*

class LocationUpdatesBroadcastReceiver : BroadcastReceiver(){

    companion object {
        private const val TAG = "LUBroadcastReceiver"
        internal const val ACTION_PROCESS_UPDATES = "com.meteocool.backgroundlocationupdates.action" + ".PROCESS_UPDATES"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent != null) {
            val action = intent.action
            if (ACTION_PROCESS_UPDATES == action) {
                val result = LocationResult.extractResult(intent)
                if (result != null) {
                    val location : Location = result.lastLocation
                    val lastLocation = LocationResultHelper.getSavedLocationResult(context)
                    val lastLat = lastLocation.getValue(LocationResultHelper.KEY_LOCATION_UPDATES_RESULT_LAT)
                    val lastLon = lastLocation.getValue(LocationResultHelper.KEY_LOCATION_UPDATES_RESULT_LON)
                    val lastAcc = lastLocation.getValue(LocationResultHelper.KEY_LOCATION_UPDATES_RESULT_ACC)
                    val isDistanceBiggerThan500F = getDistanceToLastLocation(location, context) > 499f
                       if(isDistanceBiggerThan500F){
                            Log.d(TAG, "Is distance bigger than 500f: $isDistanceBiggerThan500F")
                            Log.d(TAG, "$location is better than $lastLocation")
                            UploadLocation().execute(location)
                           if(LocationResultHelper.isExternalStorageWritable()){
                               LocationResultHelper.writeToSDFile(LocationResultHelper.getCurrentTime() + TAG + "Is distance bigger than 500f: $isDistanceBiggerThan500F")
                               LocationResultHelper.writeToSDFile(LocationResultHelper.getCurrentTime() + TAG + "[${location.latitude}, ${location.longitude}, ${location.accuracy}] is better than [$lastLat, $lastLon, $lastAcc]")
                               LocationResultHelper.writeToSDFile(LocationResultHelper.getCurrentTime() + TAG + "[${location.latitude}, ${location.longitude}, ${location.accuracy}] pushed")
                           }
                        }else{
                           if(LocationResultHelper.isExternalStorageWritable()) {
                               LocationResultHelper.writeToSDFile(LocationResultHelper.getCurrentTime() + TAG + "[${location.latitude}, ${location.longitude}, ${location.accuracy}] is not better than [$lastLat, $lastLon, $lastAcc]")

                           }
                            Log.d(TAG, "$location is not better than $lastLocation")
                        }
                    val locationResultHelper = LocationResultHelper(context, location)
                    // Save the location data to SharedPreferences.
                    locationResultHelper.saveResults()
                    Log.d(TAG, LocationResultHelper.getSavedLocationResult(context).toString())
                }
            }
        }
    }

}
