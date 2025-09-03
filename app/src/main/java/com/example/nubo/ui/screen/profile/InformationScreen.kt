package com.example.nubo.ui.screen.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey20
import com.example.nubo.ui.theme.Grey1000
import com.example.nubo.ui.theme.Grey500
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.Purple300
import com.example.nubo.ui.theme.Grey30
import com.example.nubo.ui.theme.Grey50


@Composable
fun InformationScreen(
    navController: NavController,
    name: String = "김누보",
    email: String = "nubokim@gmail.com",
    onBack: () -> Unit = {},
    onEditProfileImage: () -> Unit = {},
    onLogout: () -> Unit = {},
    onWithdraw: () -> Unit = {},
    onEditName: (String) -> Unit = {}
) {
    //네비에서 받아오는 정보
     // NavController 주입받는 부분 (혹은 파라미터)
    var currentName by rememberSaveable { mutableStateOf(name) }

    // EditNameScreen에서 수정값을 받아오기
    LaunchedEffect(Unit) {
        navController.currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<String>("edited_name")
            ?.observeForever { updated ->
                currentName = updated
            }
    }
    Scaffold(
        topBar = {
            TopBar(onBack = onBack)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(3.dp))

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
                name = currentName,
                email = email,
                onLogout = onLogout,
                onWithdraw = onWithdraw,
                onEditName = onEditName
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
            .padding(horizontal = 16.dp, vertical = 15.dp)
    ) {
        // 뒤로가기 버튼
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                .clickable(onClick = onBack)
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .align(Alignment.CenterStart),
                painter = painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = "뒤로",
                tint = Grey1000
            )
        }

        Text(
            text = "내 정보",
            style = AppTextStyles.subtitle_semibold_20,
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
    onWithdraw: () -> Unit,
    onEditName: (String) -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Grey50,                     // 원하는 컬러
                shape = RoundedCornerShape(18.dp)    // Surface와 같은 모서리 값
            )
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
            // 이름
            Text("이름", style = AppTextStyles.b1_regular_18, color = GreyMain300)
            Spacer(Modifier.height(16.dp))
            Text(
                name,
                style = AppTextStyles.b1_semibold_18,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                .fillMaxWidth()
                .clickable { onEditName(name) } )

            Divider(Modifier.padding(top = 8.dp, bottom = 36.dp), color = Grey30)

            // 메일
            Text("연동된 메일", style = AppTextStyles.b1_regular_18, color = GreyMain300)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp) // 전체 크기
                        .clip(CircleShape)
                        .border(1.dp, Grey30, CircleShape), // 원형 스트로크
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.btn_google_logo),
                        contentDescription = "Google",
                        modifier = Modifier.size(18.dp), // 아이콘 크기 (Box 내부에서 여백 있게)
                        tint = Color.Unspecified         // 원본 컬러 유지
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(email, style = AppTextStyles.b1_semibold_18, color = MaterialTheme.colorScheme.onSurface)
            }

            Divider(Modifier.padding(top = 8.dp, bottom = 45.dp), color = Grey30)

            // 로그아웃 버튼
            OutlinedButton(
                onClick = onLogout,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(104.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Grey20
                ),
                border = BorderStroke(1.dp, Grey50)
            ) {
                Text("로그아웃", style = AppTextStyles.b2_medium_16, color = Grey500)
            }

            Spacer(Modifier.height(8.dp))

            // 탈퇴하기
            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable(onClick = onWithdraw),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("탈퇴하기", style = AppTextStyles.b2_regular_16, color = GreyMain300)
                Spacer(Modifier.width(3.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_forward),
                    contentDescription = null,
                    tint = GreyMain300,
                    modifier = Modifier.size(12.dp)
                )
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

// 프로필(128 + 흰3 + 회색1) + 편집 버튼 (작은 그림자)
@Composable
private fun ProfileAvatarMini(
    imageSize: Dp,
    purple: Color,
    onEdit: () -> Unit
) {
    val strokeGrey = 1.dp
    val strokeWhite = 3.dp
    val outer = imageSize + (strokeGrey + strokeWhite) * 2

    Box(
        modifier = Modifier.size(outer),
        contentAlignment = Alignment.BottomEnd
    ) {
        // 회색 링
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Grey30, CircleShape)
                .padding(strokeGrey)
                .shadow(6.dp, CircleShape, clip = true)
        ) {
            // 흰 링
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White, CircleShape)
                    .padding(strokeWhite)
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

