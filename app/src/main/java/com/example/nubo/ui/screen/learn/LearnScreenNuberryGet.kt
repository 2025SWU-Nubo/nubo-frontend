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
import com.example.nubo.ui.theme.Grey500
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
            .background(Color.Black.copy(alpha = 0.5f))
            .noRippleClickable { /* 배경 클릭 막기 */ },
        contentAlignment = Alignment.Center
    ) {
        // 팝업 카드
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 130.dp)
                .fillMaxWidth(0.82f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(14.dp))
                .background(Purple100)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {

            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // 닫기 버튼 (우측 상단)
                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .noRippleClickable { onDismiss() }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "닫기",
                        tint = Grey1000,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // 이미지
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .graphicsLayer {
                            scaleX = scaleAnim.value
                            scaleY = scaleAnim.value
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .shadow(10.dp, CircleShape)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.learn_nuberry_get),
                            contentDescription = null,
                            modifier = Modifier.size(150.dp)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // 타이틀
                Text(
                    text = "Congratulations!",
                    style = AppTextStyles.title_semibold_24.copy(
                        brush = Brush.linearGradient(listOf(Color(0xFF8380FF), PurpleMain500))
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                // 서브텍스트
                Text(
                    text = "베리를 수확했어요.",
                    style = AppTextStyles.b2_medium_16,
                    color = Grey500,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(5.dp))
                // 서브텍스트
                Text(
                    text = "다음 성장의 새싹도 함께 키워요.",
                    style = AppTextStyles.b2_medium_16,
                    color = Grey500,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(56.dp))

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
                        text = "모은 베리 확인하기",
                        style = AppTextStyles.label_semibold_14,
                        color = Color.White
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
