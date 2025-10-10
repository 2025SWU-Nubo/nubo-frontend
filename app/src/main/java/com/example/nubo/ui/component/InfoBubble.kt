package com.example.nubo.ui.component

import androidx.annotation.DrawableRes
import com.example.nubo.R
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey700
import com.example.nubo.ui.theme.GreyMain300

class SpeechBubbleShape(
    private val cornerRadius: Dp = 15.dp,     // Bubble corner radius
    private val tailWidth: Dp = 10.dp,        // Tail width (horizontal)
    private val tailHeight: Dp = 12.dp,       // Tail height (vertical)
    private val tailOnRight: Boolean = true,  // Tail side
    private val tailCenterYFraction: Float = 0.3f // Tail vertical anchor (0f..1f)
) : Shape{
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        with(density) {
            val r = cornerRadius.toPx()
            val tW = tailWidth.toPx()
            val tH = tailHeight.toPx()

            // Guard for extremely small sizes
            val bodyWidth = if (tailOnRight) size.width - tW else size.width - tW
            val w = max(0f, bodyWidth)
            val h = size.height

            // Tail Y center
            val cy = (h * tailCenterYFraction).coerceIn(r + 4f, h - r - 4f)

            val left = if (tailOnRight) 0f else tW
            val right = if (tailOnRight) w else size.width
            val top = 0f
            val bottom = h

            val path = Path()

            // Rounded rect (manual)
            path.moveTo(left + r, top)
            path.lineTo(right - r, top)
            path.quadraticBezierTo(right, top, right, top + r)
            path.lineTo(right, bottom - r)
            path.quadraticBezierTo(right, bottom, right - r, bottom)
            path.lineTo(left + r, bottom)
            path.quadraticBezierTo(left, bottom, left, bottom - r)
            path.lineTo(left, top + r)
            path.quadraticBezierTo(left, top, left + r, top)

            // Add tail
            if (tailOnRight) {
                // Right-side tail: points outward to the right
                val baseY1 = cy - tH / 2f
                val baseY2 = cy + tH / 2f
                path.moveTo(right, baseY1)
                path.lineTo(right + tW, cy)  // tip
                path.lineTo(right, baseY2)
                path.close()
            } else {
                // Left-side tail
                val baseY1 = cy - tH / 2f
                val baseY2 = cy + tH / 2f
                path.moveTo(left, baseY1)
                path.lineTo(left - tW, cy)   // tip
                path.lineTo(left, baseY2)
                path.close()
            }

            return Outline.Generic(path)
        }
    }
}

@Composable
fun InfoBubble(
    title: String,
    subtitleLeft: String,
    centerValue: String,
    subtitleCenter: String,
    subtitleRight: String,
    @DrawableRes savedPlatformResId: Int,
    modifier: Modifier = Modifier,
    tailOnRight: Boolean = true
) {

    Surface(
        shape = SpeechBubbleShape(
            cornerRadius = 10.dp,
            tailWidth = 10.dp,
            tailHeight = 12.dp,
            tailOnRight = tailOnRight,
            tailCenterYFraction = 0.3f
        ),
        tonalElevation = 2.dp,
        shadowElevation = 8.dp,
        color = Color.White.copy(0.8f),
        modifier = modifier
    ) {
        // 전체 내부 콘텐츠
        Row(
            modifier = Modifier
                .padding( horizontal = 8.dp, vertical = 12.dp)
                .height(IntrinsicSize.Min), // for vertical dividers
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ai 카테고리 컨테이네
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = AppTextStyles.label_medium_14
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitleLeft,
                    style = AppTextStyles.caption_regular_9,
                    color = Grey700
                )
            }

            VerticalDivider()

            // 저장 날짜 컨테이너
            Column(
                Modifier
                    .weight(0.6f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = centerValue,
                    style = AppTextStyles.label_medium_14
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitleCenter,
                    style = AppTextStyles.caption_regular_9,
                    color = Grey700
                )
            }

            VerticalDivider()

            // 저장 플랫폼 컨테이너
            Column(
                Modifier
                    .weight(0.6f)
                    .padding(end = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                    Icon(
                        painter = painterResource(id = savedPlatformResId),
                        contentDescription = "뒤로가기",
                        tint = Grey700,
                        modifier = Modifier.size(20.dp)
                    )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitleRight,
                    style = AppTextStyles.caption_regular_9,
                    color = Grey700
                )
            }
        }
    }
}

// 카테고리 분리 선
@Composable
private fun VerticalDivider() {
    Box(
        Modifier
            .height(18.dp)
            .width(1.dp)
            .background(GreyMain300)
            .padding(horizontal = 4.dp)
    )
}

// 프리뷰
@Preview(showBackground = true)
@Composable
private fun InfoBubblePreview() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            InfoBubble(
                title = "#요리 레시피",
                subtitleLeft = "AI 카테고리",
                centerValue = "2024.03.03",
                subtitleCenter = "저장한 날짜",
                subtitleRight = "저장 플랫폼",
                savedPlatformResId = R.drawable.youtube_logo,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
