package com.example.nubo.ui.screen.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey1000
import com.example.nubo.ui.theme.Grey20
import com.example.nubo.ui.theme.Purple300

@Composable
fun ProfileScreen(
    nickname: String = "김누보",
    email: String = "nubokim@gmail.com",
    onBack: () -> Unit = {},
    onBellClick: () -> Unit = {},
    onEditProfileImage: () -> Unit = {},
    onMyInfo: () -> Unit = {},
    onNotification: () -> Unit = {},
    onHelp: () -> Unit = {},
    onPrivacy: () -> Unit = {},
) {
    // ---- 레이아웃 기준값 ----
    val headerHeight = 344.dp        // 상단 헤더 높이
    val profileImageSize = 128.dp    // 사진 크기(정확히 128dp)

    Box(Modifier.fillMaxSize()) {

        // ===== 1) 상단 헤더(그라데이션/벡터 배경) =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
        ) {
            // 배경 이미지
            Image(
                painter = painterResource(R.drawable.bg_profile_header),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
            // 상태바 안전영역 안의 아이콘(뒤로/알림)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 뒤로가기 버튼
                Box(
                    modifier = Modifier
                        .size(38.dp) // 버튼 크기
                        .background(Color.Black.copy(alpha = 0.05f), CircleShape) // 반투명 원 배경
                        .clickable(onClick = onBack), // 클릭 이벤트
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = "뒤로",
                        tint = Grey1000,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 알림 버튼
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.Black.copy(alpha = 0.05f), CircleShape)
                        .clickable(onClick = onBellClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_profile_bell),
                        contentDescription = "알림",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            //프로필 클러스터
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 112.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 사진 128 + 보라 1 + 흰 1 (ProfileAvatar가 그려줌)
                ProfileAvatar(
                    imageSize = profileImageSize,
                    onEdit = onEditProfileImage,
                    purple = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(12.dp))

                //닉네임
                Text(
                    text = nickname,
                    style = AppTextStyles.title_bold_24,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(4.dp))

                //이메일
                Text(text = email, style = AppTextStyles.b2_regular_16, color = MaterialTheme.colorScheme.secondary)
            }
        }

        // ===== 3) 하단 흰 패널: 헤더 바로 아래에서 시작(겹침 없음) =====
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = headerHeight),           // ← 헤더 높이만큼부터 시작
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            ) {

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "설정",
                    style = AppTextStyles.b2_regular_16,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 15.dp)
                )

                SettingsItem(title = "내 정보", onClick = onMyInfo)
                SettingsItem(title = "알림", onClick = onNotification)

                Spacer(Modifier.height(15.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(Grey20)   // 원하는 배경색
                )

                Spacer(Modifier.height(15.dp))

                SettingsItem(title = "도움말", onClick = onHelp)
                SettingsItem(title = "개인정보 처리방침", onClick = onPrivacy)
            }
        }
    }
}

// 프로필 영역
@Composable
private fun ProfileAvatar(
    imageSize: Dp,            // 정확히 128.dp
    purple: Color,            // 보라색(테마 토큰)
    onEdit: () -> Unit
) {
    val strokeWhite = 2.dp
    val strokePurple = 2.dp
    val outerSize = imageSize + (strokeWhite + strokePurple) * 2  // 128 + 1*2 + 1*2 = 132

    Box(
        modifier = Modifier.size(outerSize),   // 부모는 clip 걸지 않음 → 버튼이 밖으로 나올 수 있음
        contentAlignment = Alignment.BottomEnd
    ) {
        // 바깥 흰 링
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White, shape = CircleShape)
                .padding(strokeWhite)          // 흰 2dp 만큼 내부로
        ) {
            // 안쪽 보라 링
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Purple300, shape = CircleShape)
                    .padding(strokePurple)     // 보라 2dp 만큼 내부로
            ) {
                // 실제 사진
                Image(
                    painter = painterResource(id = R.drawable.profile_image),
                    contentDescription = "프로필",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),    // 이미지에만 원형 클립
                    contentScale = ContentScale.Crop
                )
            }
        }

        // 편집 버튼
        Box(
            modifier = Modifier
                .offset(x = -3.dp, y = -3.dp)
                .size(34.dp) // 버튼 배경 크기
                .shadow(3.dp, CircleShape, clip = true)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .clickable(onClick = onEdit),    // Box + clickable
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_profile_edit),
                contentDescription = "프로필 편집",
                modifier = Modifier.size(24.dp),   // 아이콘 크기 고정
                tint = Color.Unspecified
            )
        }
    }
}

// 설정 텍스트 영역
@Composable
private fun SettingsItem(
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 32.dp, end = 24.dp, top = 15.dp, bottom = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = AppTextStyles.b1_semibold_18,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_arrow_forward),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}
