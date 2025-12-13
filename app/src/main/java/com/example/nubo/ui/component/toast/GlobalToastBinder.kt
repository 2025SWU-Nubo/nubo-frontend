package com.example.nubo.ui.component.toast

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.AnnotatedString
import com.example.components.toast.AppToastHostState
import com.example.components.toast.AppToastLayout
import kotlinx.coroutines.launch

@Composable
fun GlobalToastBinder(
    hostState: AppToastHostState,
) {
    DisposableEffect(Unit) {
        GlobalToastBus.setHostReady(true)
        onDispose { GlobalToastBus.setHostReady(false) }
    }

    LaunchedEffect(Unit) {
        GlobalToastBus.events.collect { event ->
            hostState.show(
                title = AnnotatedString(event.message),
                layout = event.layout,
                type = event.type,
                durationMillis = event.durationMillis,
                preDelayMillis = event.preDelayMillis,
                actionLabel = event.actionLabel,
                onAction = event.onAction
            )
        }
    }
}
