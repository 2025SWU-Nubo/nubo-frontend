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
    Scaffold(
        topBar = {
            TopBar(onBack = onBack)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(24.dp))

            // ===== 프로필 영역 =====
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

            Spacer(Modifier.height(32.dp)) // 프로필과 카드 사이 여백 크게

            // ===== 카드 영역 =====
            InfoCard(
                name = name,
                email = email,
                onLogout = onLogout,
                onWithdraw = onWithdraw
            )
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 뒤로가기 버튼
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

        Text(
            text = "내 정보",
            style = AppTextStyles.title_bold_24,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun InfoCard(
    name: String,
    email: String,
    onLogout: () -> Unit,
    onWithdraw: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            // 이름
            Text("이름", style = AppTextStyles.b2_regular_16, color = Grey500)
            Spacer(Modifier.height(8.dp))
            Text(name, style = AppTextStyles.b1_semibold_18, color = Grey1000)

            Divider(Modifier.padding(vertical = 16.dp), color = GreyMain300)

            // 메일
            Text("연동된 메일", style = AppTextStyles.b2_regular_16, color = Grey500)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ic_profile_google),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(email, style = AppTextStyles.b1_semibold_18, color = Grey1000)
            }

            Divider(Modifier.padding(vertical = 16.dp), color = GreyMain300)

            // 로그아웃 버튼
            OutlinedButton(
                onClick = onLogout,
                shape = CircleShape,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Grey20,
                    contentColor = Grey1000
                )
            ) {
                Text("로그아웃", style = AppTextStyles.b2_semibold_16)
            }

            Spacer(Modifier.height(8.dp))

            // 탈퇴하기
            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable(onClick = onWithdraw),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("탈퇴하기", style = AppTextStyles.b2_regular_16, color = Grey500)
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
}

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

