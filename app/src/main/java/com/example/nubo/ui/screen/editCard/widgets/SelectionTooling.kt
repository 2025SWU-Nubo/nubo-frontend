package com.example.nubo.ui.screen.editCard.widgets

import androidx.compose.runtime.*
import androidx.compose.ui.platform.*

@Composable
fun NoSelectionToolbar(content: @Composable () -> Unit) {
    val noToolbar = remember {
        object : TextToolbar {
            override val status = TextToolbarStatus.Hidden
            override fun showMenu(rect: androidx.compose.ui.geometry.Rect, onCopyRequest: (() -> Unit)?, onPasteRequest: (() -> Unit)?, onCutRequest: (() -> Unit)?, onSelectAllRequest: (() -> Unit)?) {}
            override fun hide() {}
        }
    }
    CompositionLocalProvider(LocalTextToolbar provides noToolbar) { content() }
}
