package com.example.nubo.ui.screen.onBoadingTutorial

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.nubo.R
import com.example.nubo.data.model.OnboardingPage
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.PurpleMain500


@Composable
fun OnboardingOutroPage(
    stepNumber: Int,
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 실제 텍스트와 카드 일러스트 영역
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(50.dp))

            // 페이지 번호
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(
                        color = PurpleMain500.copy(alpha = 0.6f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber.toString(),
                    style = AppTextStyles.b1_bold_18,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 제목 고정
            Text(
                text = "Nubo에 공유하기만 하면\n영상 카드 추가가 완료!",
                style = AppTextStyles.title_bold_24,
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 카드 일러스트 남은 영역 중앙에
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),                  // 중간 영역
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.onboarding_outro_card),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .heightIn(max = 260.dp)
                )
            }

            // 안내 카드
            OutroLinkGuideCard()

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

    @Composable
    fun OutroLinkGuideCard() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(10.dp)
                )
        ){
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "이미 복사한 링크가 있다면?",
                    style = AppTextStyles.b1_semibold_18,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 설명 1줄 + 아이콘 들어가는 부분
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "추가 탭을 눌러",
                        style = AppTextStyles.b3_medium_14,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // 영상 아이콘 (디자인된 PNG 있으면 교체)
                    Image(
                        painter = painterResource(id = R.drawable.add_card_icon),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "을 선택하면,",
                        style = AppTextStyles.b3_medium_14,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .height(16.dp)
                        .width(1.dp)
                        .background(Color(0xFFCAD5FF))
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 설명 2줄
                Text(
                    text = "복사한 링크를 붙여넣어\n영상 카드를 추가할 수 있어요.",
                    style = AppTextStyles.b3_medium_14,
                    textAlign = TextAlign.Center,
                    color = Color.Black
                )
            }
        }
    }

