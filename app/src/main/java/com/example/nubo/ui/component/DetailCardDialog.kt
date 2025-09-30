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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import com.example.nubo.R
import com.example.nubo.model.card.CardDetailItem
import com.example.nubo.ui.screen.home.HomeScreen
import com.example.nubo.ui.theme.NuboAppTheme
import kotlinx.coroutines.delay


@Composable
fun DetailCardDialog(
    item: CardDetailItem,
    onDismiss: () -> Unit
) {

    val isPreview = LocalInspectionMode.current

    var flipped by remember { mutableStateOf(false) }
    var isPreparing by remember { mutableStateOf(isPreview) }

    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
    )
    val density = LocalDensity.current.density

    // 자동 flip 트리거
    if(!isPreview){
        LaunchedEffect(Unit) {
            delay(2000L) //데이터 준비 시간
            isPreparing = false
            delay(3000L) // 5초 기다림
            flipped = true
        }
    }


    Dialog(onDismissRequest = { onDismiss() }) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .width(500.dp)
                    .height(600.dp)
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12 * density
                    },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(16.dp)
            ) {
                if (isPreparing) {
                    // 로딩 화면
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else{
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
}

//프리뷰일 경우 delay 작동 안하게
//@Composable
//fun isPreview(): Boolean {
//    return LocalInspectionMode.current
//}

//@Preview(showBackground = true, showSystemUi = true)
//@Composable
//fun DetailCardDialogScreenPreview() {
//    NuboAppTheme {
//        val sample = listOf("전자렌지 요리","자취 요리","계란밥")
//        // 샘플 데이터 생성
//        val sampleItem = CardDetailItem(
//            icad = 12344,
//            imageUrl = "https://picsum.photos/400/300",
//            videoUrl = "",
//            title = "샘플 카드",
//            category = "AI 개발",
//            boardSource = "AI",
//            description = "이것은 샘플 카드 설명입니다.",
//            date = "2025.06.11",
//            videoPlatform = "youtube",
//            tags = sample
//        )
//
//
//        DetailCardDialog(sampleItem,
//            onDismiss = {})
//    }
//}

