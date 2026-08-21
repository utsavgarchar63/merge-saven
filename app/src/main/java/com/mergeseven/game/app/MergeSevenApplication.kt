package com.mergeseven.game.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Merge Seven.
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation.
 */
@HiltAndroidApp
class MergeSevenApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Future initialization:
        // - Firebase
        // - Crashlytics
        // - AdMob
        // - Analytics
    }
}
