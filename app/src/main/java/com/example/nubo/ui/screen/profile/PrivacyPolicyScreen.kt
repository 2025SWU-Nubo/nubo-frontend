package com.example.nubo.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey10
import com.example.nubo.ui.theme.Grey1000
import com.example.nubo.ui.theme.Grey50

data class PolicySection(
    val title: String,
    val body: String
)

@Composable
fun PrivacyPolicyRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {

    // English comment: Replace with real policy text later
    val sections = remember {
        listOf(
            PolicySection(
                title = "1. 개인정보 처리 목적",
                body = "서비스 제공 및 운영\n고객 문의 응대\n서비스 개선 및 품질 관리"
            ),
            PolicySection(
                title = "2. 수집하는 개인정보 항목",
                body = "필수: 이메일, 닉네임, 프로필 이미지\n선택: 앱 이용 과정에서 생성되는 로그 정보"
            ),
            PolicySection(
                title = "3. 보유 및 이용 기간",
                body = "회원 탈퇴 시 지체 없이 파기돼요.\n법령에 따라 보관이 필요한 경우 해당 기간 동안 보관해요."
            ),
            PolicySection(
                title = "4. 제3자 제공",
                body = "원칙적으로 제공하지 않아요\n단, 법령 근거가 있는 경우 제공될 수 있어요"
            ),
            PolicySection(
                title = "5. 처리 위탁",
                body = "서비스 운영에 필요한 범위에서 위탁할 수 있어요.\n위탁 시 안전하게 관리해요."
            ),
            PolicySection(
                title = "6. 이용자 권리",
                body = "열람, 정정, 삭제, 처리정지 요청 가능해요.\n문의 채널을 통해 요청할 수 있어요."
            ),
            PolicySection(
                title = "7. 문의처",
                body = "이메일: nubo@gmail.com\n운영 시간: 평일 10:00 ~ 18:00"
            ),
            PolicySection(
                title = "부칙",
                body = "시행일: 2025-12-18"
            )
        )
    }

    PrivacyPolicyScreen(
        title = "개인정보 처리방침",
        sections = sections,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    title: String,
    sections: List<PolicySection>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = title,
                        style = AppTextStyles.subtitle_semibold_20
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        focusManager.clearFocus(force = true)
                        onBack()
                    }) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = "뒤로가기")
                    }
                },
                modifier = Modifier.drawBehind {
                    val y = size.height
                    drawLine(
                        color = Grey50,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end   = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState())

        ) {
            Text(
                text = "본 방침은 서비스 이용과 관련된 개인정보 처리 기준을 안내해요.",
                style = AppTextStyles.b2_regular_16,
                color = MaterialTheme.colorScheme.secondary,
                modifier= Modifier.padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 20.dp,)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Grey10)
            )
            Spacer(Modifier.height(32.dp))

            SelectionContainer {
                Column(
                    modifier= Modifier.padding(horizontal = 24.dp)
                ) {
                    sections.forEachIndexed { index, section ->
                        Text(
                            text = section.title,
                            style = AppTextStyles.b1_semibold_18,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = section.body,
                            style = AppTextStyles.b2_regular_16,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        if (index != sections.lastIndex) {
                            Spacer(Modifier.height(16.dp))
//                            Divider()
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}
