package com.meteocool

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import com.meteocool.settings.SettingsActivity

class SplashActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

            startActivity(Intent(this.applicationContext, SettingsActivity::class.java))
//        if(!isOnboardingCompleted()) {
//            startActivity(Intent(this.applicationContext, OnboardingActivity::class.java))
//        }else {
//            startActivity(Intent(this.applicationContext, MeteocoolActivity::class.java))
//        }
        finish()
    }

    private fun isOnboardingCompleted() : Boolean {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(OnboardingActivity.IS_ONBOARD_COMPLETED, false)
    }
}
