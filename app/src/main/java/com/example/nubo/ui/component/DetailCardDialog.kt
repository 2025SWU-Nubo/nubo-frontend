package com.example.nubo.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import com.example.nubo.model.card.CardDetailDialogItem
import kotlinx.coroutines.delay


@Composable
fun DetailCardDialog(
    item: CardDetailDialogItem,
    onDismiss: () -> Unit
) {


    var flipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
    )
    val density = LocalDensity.current.density

    // 자동 flip 트리거
    LaunchedEffect(Unit) {
        delay(5000L) // 5초 기다림
        flipped = true
    }

    Dialog(onDismissRequest = { onDismiss() }) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .width(330.dp)
                    .height(550.dp)
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12 * density
                    },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(16.dp)
            ) {
                if (rotation <= 90f) {
                    DetailCardFront(
                        item = item,
                        onDismiss = onDismiss,
                        onFlip = { flipped = true }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationY = 180f }
                    ) {
                        DetailCardBack(
                            item = item,
                            onDismiss = onDismiss,
                            onFlip = { flipped = false}
                        )
                    }
                }
            }
        }
    }
}
