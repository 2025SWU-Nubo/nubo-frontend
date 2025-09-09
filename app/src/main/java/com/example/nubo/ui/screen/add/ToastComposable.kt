package com.example.nubo.ui.screen.add

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey1000
import com.example.nubo.ui.theme.Grey500
import kotlinx.coroutines.delay

@Composable
fun SheetTopToast(
    title: AnnotatedString,
    message: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    bottomOffset: Dp = 180.dp,
    durationMillis: Long = 3000L // 기본 3초
) {
    if (visible) {
        // 자동 dismiss
        LaunchedEffect(Unit) {
            delay(durationMillis)
            onDismiss()
        }

        val density = LocalDensity.current
        val offsetY = with(density) { bottomOffset.roundToPx() }

        Popup(
            alignment = Alignment.BottomCenter,
            offset = IntOffset(0, -offsetY)   // 아래 기준 위로 올림
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp), // 모달 위쪽 여백
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = 4.dp,
                            spotColor = Color(0x17000000),
                            ambientColor = Color(0x17000000)
                        )
                        .width(391.dp)
                        .height(95.dp)
                        .background(
                            color = Color(0xE6FFFFFF),
                            shape = RoundedCornerShape(size = 16.dp)
                        )
                        .padding(
                            start = 16.dp,
                            top = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = title,
                            style = AppTextStyles.b2_bold_16,   // Title style
                            color = Grey1000,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))   // 타이틀-메시지 간격

                        Text(
                            text = message,
                            style = AppTextStyles.b3_regular_14,    // Message style
                            color = Grey500,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

