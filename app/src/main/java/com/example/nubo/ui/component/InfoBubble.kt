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
import androidx.compose.ui.text.style.TextAlign
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey200
import com.example.nubo.ui.theme.Grey700
import com.example.nubo.ui.theme.GreyMain300

/**
 * 말풍선 모양 Shape (꼬리 포함)
 * @param cornerRadius 버블 모서리 둥글기
 * @param tailWidth 꼬리 너비 (가로)
 * @param tailHeight 꼬리 높이 (세로)
 * @param tailOnRight 꼬리 방향 (true: 오른쪽, false: 왼쪽)
 * @param tailCenterYFraction 꼬리의 세로 위치 (0f = 상단, 1f = 하단)
 */
class SpeechBubbleShape(
    private val cornerRadius: Dp = 15.dp,
    private val tailWidth: Dp = 10.dp,
    private val tailHeight: Dp = 12.dp,
    private val tailOnRight: Boolean = true,
    private val tailCenterYFraction: Float = 0.3f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        with(density) {
            val r = cornerRadius.toPx()
            val tW = tailWidth.toPx()
            val tH = tailHeight.toPx()

            // 극단적으로 작은 크기 방어 처리
            val bodyWidth = if (tailOnRight) size.width - tW else size.width - tW
            val w = max(0f, bodyWidth)
            val h = size.height

            // 꼬리 Y축 중심점
            val cy = (h * tailCenterYFraction).coerceIn(r + 4f, h - r - 4f)

            val left = if (tailOnRight) 0f else tW
            val right = if (tailOnRight) w else size.width
            val top = 0f
            val bottom = h

            val path = Path()

            // 둥근 사각형 그리기 (수동)
            path.moveTo(left + r, top)
            path.lineTo(right - r, top)
            path.quadraticBezierTo(right, top, right, top + r)
            path.lineTo(right, bottom - r)
            path.quadraticBezierTo(right, bottom, right - r, bottom)
            path.lineTo(left + r, bottom)
            path.quadraticBezierTo(left, bottom, left, bottom - r)
            path.lineTo(left, top + r)
            path.quadraticBezierTo(left, top, left + r, top)

            // 꼬리 추가
            if (tailOnRight) {
                // 오른쪽 꼬리: 오른쪽으로 튀어나옴
                val baseY1 = cy - tH / 2f
                val baseY2 = cy + tH / 2f
                path.moveTo(right, baseY1)
                path.lineTo(right + tW, cy)  // 끝점
                path.lineTo(right, baseY2)
                path.close()
            } else {
                // 왼쪽 꼬리
                val baseY1 = cy - tH / 2f
                val baseY2 = cy + tH / 2f
                path.moveTo(left, baseY1)
                path.lineTo(left - tW, cy)   // 끝점
                path.lineTo(left, baseY2)
                path.close()
            }

            return Outline.Generic(path)
        }
    }
}

/**
 * 정보 표시용 말풍선 컴포넌트
 * 카테고리, 저장 날짜, 저장 플랫폼을 균등하게 표시
 */
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
                .padding(horizontal = 8.dp, vertical = 10.dp)
                .height(IntrinsicSize.Min), // 구분선 높이 자동 조정
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp) // 균등 간격
        ) {
            // ── AI 카테고리 컨테이너 ──
            InfoSection(
                mainText = title,
                subText = subtitleLeft,
                modifier = Modifier.weight(1f)
            )

            VerticalDivider()

            // ── 저장 날짜 컨테이너 ──
            InfoSection(
                mainText = centerValue,
                subText = subtitleCenter,
                modifier = Modifier.weight(1f)
            )

            VerticalDivider()

            // ── 저장 플랫폼 컨테이너 ──
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .padding(start = 4.dp, end = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = savedPlatformResId),
                    contentDescription = "저장 플랫폼",
                    tint = Grey700,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitleRight,
                    style = AppTextStyles.caption_regular_9,
                    color = Grey700,
                    textAlign = TextAlign.Center, // 중앙 정렬
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 정보 섹션 컴포저블 (재사용)
 * 제목과 부제목을 수직 중앙 정렬하여 표시
 */
@Composable
private fun InfoSection(
    mainText: String,
    subText: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center // 수직 중앙 정렬
    ) {
        Text(
            text = mainText,
            style = AppTextStyles.label_medium_14,
            textAlign = TextAlign.Center, // 텍스트 중앙 정렬
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subText,
            style = AppTextStyles.caption_regular_9,
            color = Grey700,
            textAlign = TextAlign.Center, // 텍스트 중앙 정렬
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 카테고리 구분 세로선
 */
@Composable
private fun VerticalDivider() {
    Box(
        Modifier
            .fillMaxHeight() // Row의 높이에 맞춤
            .width(1.dp)
            .padding(vertical = 8.dp)
            .background(Grey200)
    )
}

// ── 프리뷰 ──
@Preview(showBackground = true, widthDp = 320)
@Composable
private fun InfoBubblePreview() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 일반 케이스
            InfoBubble(
                title = "#요리 레시피",
                subtitleLeft = "AI 카테고리",
                centerValue = "2024.03.03",
                subtitleCenter = "저장한 날짜",
                subtitleRight = "저장 플랫폼",
                savedPlatformResId = R.drawable.youtube_logo
            )

            // 긴 제목 케이스 (줄바꿈 테스트)
            InfoBubble(
                title = "#인공지능과 머신러닝의 미래",
                subtitleLeft = "AI 카테고리",
                centerValue = "2024.12.25",
                subtitleCenter = "저장한 날짜",
                subtitleRight = "저장 플랫폼",
                savedPlatformResId = R.drawable.youtube_logo
            )

            // 왼쪽 꼬리 케이스
            InfoBubble(
                title = "#디자인",
                subtitleLeft = "AI 카테고리",
                centerValue = "2025.01.15",
                subtitleCenter = "저장한 날짜",
                subtitleRight = "저장 플랫폼",
                savedPlatformResId = R.drawable.youtube_logo,
                tailOnRight = false
            )
        }
    }
}
