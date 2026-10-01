package com.mergeseven.game.core

import android.app.Activity
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/** The Games SDK needs an Activity; an Application context cannot supply one. */
@Singleton
class ForegroundActivity @Inject constructor() {
    private var reference = WeakReference<Activity>(null)
    fun attach(activity: Activity) { reference = WeakReference(activity) }
    fun detach(activity: Activity) { if (reference.get() === activity) reference.clear() }
    fun current(): Activity? = reference.get()?.takeUnless { it.isFinishing || it.isDestroyed }
}
