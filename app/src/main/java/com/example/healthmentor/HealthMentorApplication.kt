package com.example.healthmentor

import android.app.Application
import com.google.android.gms.common.GoogleApiAvailability
import android.content.Context

class HealthMentorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val availability = GoogleApiAvailability.getInstance()
        val resultCode = availability.isGooglePlayServicesAvailable(this)
        if (resultCode != com.google.android.gms.common.ConnectionResult.SUCCESS) {
            availability.showErrorNotification(this, resultCode)
        }
    }
} 