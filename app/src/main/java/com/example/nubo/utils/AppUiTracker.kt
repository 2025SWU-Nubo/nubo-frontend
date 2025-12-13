package com.example.nubo.utils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.util.concurrent.atomic.AtomicInteger

object AppUiTracker : Application.ActivityLifecycleCallbacks {

    private val resumedCount = AtomicInteger(0)

    val isResumed: Boolean
        get() = resumedCount.get() > 0

    fun init(app: Application) {
        app.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityResumed(activity: Activity) {
        resumedCount.incrementAndGet()
    }

    override fun onActivityPaused(activity: Activity) {
        resumedCount.decrementAndGet()
        if (resumedCount.get() < 0) resumedCount.set(0)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
