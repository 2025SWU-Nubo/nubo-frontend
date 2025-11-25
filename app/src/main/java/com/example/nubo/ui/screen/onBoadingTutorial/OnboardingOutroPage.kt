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

// 마지막 온보딩 페이지 본문
@Composable
fun OnboardingOutroPage(
    stepNumber: Int,
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 배경 그라디언트 PNG 이미지
        Image(
            painter = painterResource(id = R.drawable.outro_bg),
            contentDescription = null,      // 장식용 이미지라서 null
            modifier = Modifier.matchParentSize(), // 화면 전체 채우기
            contentScale = ContentScale.Crop       // 비율 유지하면서 꽉 채우기
        )

        // 실제 텍스트와 카드 일러스트 영역
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
//            Spacer(modifier = Modifier.height(40.dp))

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

            Text(
                text = "Nubo에 공유하기만 하면\n영상 카드 추가가 완료!",
                style = AppTextStyles.b1_bold_18,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(24.dp))

            // TODO 카드 일러스트 PNG 배치
            Image(
                painter = painterResource(id = R.drawable.onboarding_outro_card),
                contentDescription = null
            )

            Spacer(modifier = Modifier.height(40.dp))

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
                    shape = RoundedCornerShape(10.dp)  // 흰색 라운드 카드
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

//@Preview(showBackground = true, showSystemUi = true)
//@Composable
//private fun OnboardingOutroPagePreview() {
//    MaterialTheme {
//        OnboardingOutroPage("5")
//    }
//}

