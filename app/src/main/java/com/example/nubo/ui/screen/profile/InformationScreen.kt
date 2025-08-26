package com.example.nubo.ui.screen.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.nubo.ui.theme.Grey20
import com.example.nubo.ui.theme.Grey1000
import com.example.nubo.ui.theme.Grey500
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.Purple300
import androidx.compose.ui.draw.drawBehind


@Composable
fun InformationScreen(
    name: String = "김누보",
    email: String = "nubokim@gmail.com",
    onBack: () -> Unit = {},
    onEditProfileImage: () -> Unit = {},
    onLogout: () -> Unit = {},
    onWithdraw: () -> Unit = {},
) {
    // ---- 상단 앱바: 뒤로가기 + 가운데 타이틀 ----
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 뒤로가기 (원 배경)
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color.Black.copy(alpha = 0.05f), CircleShape)
                .clickable(onClick = onBack)
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = "뒤로",
                tint = Grey1000
            )
        }
        // 가운데 타이틀
        Text(
            text = "내 정보",
            style = AppTextStyles.title_bold_24,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Center)
        )
    }

    // ---- 본문 ----
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        // 프로필 아바타(128 + 보라1 + 흰1) + 편집 버튼
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            ProfileAvatarMini(
                imageSize = 128.dp,
                purple = Purple300,
                onEdit = onEditProfileImage
            )
        }

        Spacer(Modifier.height(20.dp))

        // 카드 컨테이너
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(16.dp), clip = true)
                .borderDefault()
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {

                // 라벨
                Text(
                    text = "이름",
                    style = AppTextStyles.b2_regular_16,
                    color = Grey500
                )
                Spacer(Modifier.height(8.dp))

                // 값(굵게)
                Text(
                    text = name,
                    style = AppTextStyles.b1_semibold_18,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 구분선
                Divider(
                    color = GreyMain300,
                    thickness = 1.dp,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                // 라벨
                Text(
                    text = "연동된 메일",
                    style = AppTextStyles.b2_regular_16,
                    color = Grey500
                )
                Spacer(Modifier.height(10.dp))

                // 아이콘 + 이메일 값
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_profile_google),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = email,
                        style = AppTextStyles.b1_semibold_18,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 구분선
                Divider(
                    color = GreyMain300,
                    thickness = 1.dp,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                // 로그아웃 버튼 (라운드 필)
                OutlinedButton(
                    onClick = onLogout,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .height(44.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Grey20,
                        contentColor = Grey1000
                    )
                ) {
                    Text(text = "로그아웃", style = AppTextStyles.b2_semibold_16)
                }

                Spacer(Modifier.height(8.dp))

                // 탈퇴하기 링크
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable(onClick = onWithdraw),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "탈퇴하기",
                        style = AppTextStyles.b2_regular_16,
                        color = Grey500
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_forward),
                        contentDescription = null,
                        tint = Grey500,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/* ---------- 보조 컴포넌트 ---------- */

// 카드 테두리(연한 그레이) 공통화
private fun Modifier.borderDefault(): Modifier =
    this.then(
        Modifier
            .background(Color.Transparent, RoundedCornerShape(16.dp))
            .drawBehind {
                // 둥근 카드 외곽선
                val stroke = 1.dp.toPx()
                drawRoundRect(
                    color = GreyMain300,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                )
            }
    )

// 프로필(128 + 보라1 + 흰1) + 편집 버튼 (작은 그림자)
@Composable
private fun ProfileAvatarMini(
    imageSize: Dp,
    purple: Color,
    onEdit: () -> Unit
) {
    val strokeWhite = 1.dp
    val strokePurple = 1.dp
    val outer = imageSize + (strokeWhite + strokePurple) * 2

    Box(
        modifier = Modifier.size(outer),
        contentAlignment = Alignment.BottomEnd
    ) {
        // 흰 링
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White, CircleShape)
                .padding(strokeWhite)
        ) {
            // 보라 링
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(purple, CircleShape)
                    .padding(strokePurple)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.profile_image),
                    contentDescription = "프로필",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // 편집 버튼(작게)
        Box(
            modifier = Modifier
                .offset(x = (-4).dp, y = (-4).dp)
                .size(28.dp)
                .shadow(2.dp, CircleShape, clip = true)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .clickable(onClick = onEdit),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_profile_edit),
                contentDescription = "프로필 편집",
                tint = Color.Unspecified,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
