package com.example.nubo.ui.screen.learn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.nubo.R
import com.example.nubo.ui.component.noRippleClickable
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey1000
import com.example.nubo.ui.theme.Purple100
import com.example.nubo.ui.theme.PurpleMain500

@Composable
fun NuberryGet(
    onDismiss: () -> Unit,
    onClickBerryPage: () -> Unit
) {
    // 팝업 표시 애니메이션 (베리 살짝 커졌다 작아지는 효과)
    val scaleAnim = remember { Animatable(0.8f) }

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            1.2f,
            animationSpec = tween(400, easing = LinearEasing)
        )
        scaleAnim.animateTo(
            1f,
            animationSpec = tween(300, easing = LinearEasing)
        )
    }

    // 반투명 배경 (뒤 비활성화)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .noRippleClickable { /* 배경 클릭 막기 */ },
        contentAlignment = Alignment.Center
    ) {
        // 팝업 카드
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(14.dp))
                .background(Purple100)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 닫기 버튼 (좌측 상단)
                Box(
                    modifier = Modifier
                        .align(Alignment.Start)
                        .noRippleClickable { onDismiss() }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close), // 닫기 아이콘
                        contentDescription = "닫기",
                        tint = Grey1000,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.height(32.dp))

                // 중앙 원 + 베리 이미지
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .graphicsLayer {
                            scaleX = scaleAnim.value
                            scaleY = scaleAnim.value
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .shadow(8.dp, CircleShape)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.learn_nuberry_get),
                            contentDescription = null,
                            modifier = Modifier.size(120.dp)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // 텍스트 영역
                Text(
                    text = "Nuberry Get!",
                    style = AppTextStyles.learn_percentage_30.copy(
                        brush = Brush.linearGradient(listOf(Color(0xFF8380FF), PurpleMain500))
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "멋진 수확이에요.\n다음 성장도 함께 가요.",
                    style = AppTextStyles.b2_medium_16,
                    color = Grey1000,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(36.dp))

                // 버튼
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(41.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PurpleMain500)
                        .noRippleClickable { onClickBerryPage() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "모은 베리 확인하러 가기",
                        style = AppTextStyles.label_semibold_14,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
