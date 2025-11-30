package com.example.nubo.utils

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import android.util.Log

object AppForegroundTracker : DefaultLifecycleObserver {

    @Volatile
    var isForeground: Boolean = false
        private set

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        isForeground = true
        Log.d("AppForegroundTracker", "App moved to foreground")
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        isForeground = false
        Log.d("AppForegroundTracker", "App moved to background")
    }

    fun init() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
}
