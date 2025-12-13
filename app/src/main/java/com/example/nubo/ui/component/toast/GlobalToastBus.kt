package com.example.nubo.ui.component.toast

import com.example.components.toast.AppToastLayout
import com.example.components.toast.AppToastType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

data class GlobalToastEvent(
    val message: String,
    val type: AppToastType = AppToastType.NORMAL,
    val layout: AppToastLayout = AppToastLayout.TitleOnly,
    val durationMillis: Int = 2000,
    val preDelayMillis: Int = 400,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null, // For service usage, usually null
)

object GlobalToastBus {

    // Extra buffer so service can emit without suspending
    private val _events = MutableSharedFlow<GlobalToastEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<GlobalToastEvent> = _events

    // Whether Compose toast host is mounted and collecting events
    @Volatile
    private var hostReady: Boolean = false

    fun setHostReady(ready: Boolean) {
        hostReady = ready
    }

    fun isHostReady(): Boolean = hostReady

    fun emit(event: GlobalToastEvent) {
        android.util.Log.d("GlobalToastBus", "emit message=${event.message}, type=${event.type}")
        _events.tryEmit(event)
    }

    fun showMessage(
        message: String,
        type: AppToastType = AppToastType.NORMAL,
        durationMillis: Int = 2000,
        preDelayMillis: Int = 400,
    ) {
        emit(
            GlobalToastEvent(
                message = message,
                type = type,
                durationMillis = durationMillis,
                preDelayMillis = preDelayMillis
            )
        )
    }
}
