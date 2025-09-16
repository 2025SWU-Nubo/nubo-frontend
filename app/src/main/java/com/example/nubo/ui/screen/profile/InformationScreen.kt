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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
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
    onBack: () -> Unit = {},
    onEditProfileImage: () -> Unit = {},
    onLogout: () -> Unit = {},
    onWithdraw: () -> Unit = {},
    onEditName: (String) -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    // 서버 상태 구독
    val state = viewModel.uiState.collectAsState().value

    // 서버 값 → 기본값 폴백
    val name = state.data?.name?.takeIf { !it.isNullOrBlank() } ?: "이름 없음"
    val email = state.data?.email?.takeIf { !it.isNullOrBlank() } ?: "이메일 없음"
    val imageUrl = state.data?.profileImageUrl

    // 현재 화면에서 표시할 이름 상태 (이후 이름 수정 반영)
    var currentName by rememberSaveable { mutableStateOf(name) }

    LaunchedEffect(name) {
        currentName = name
    }

    // EditNameScreen에서 돌아온 값 반영
    val editedNameFlow = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow("edited_name", name)
    val editedName by (editedNameFlow?.collectAsState() ?: remember { mutableStateOf(name) })
    LaunchedEffect(editedName) { currentName = editedName }

    Scaffold(
        topBar = {
            TopBar(onBack = {
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("profile_name_override", currentName)

                onBack()  // 기존 onBack() 실행 (popBackStack)
            })
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
                    onEdit = onEditProfileImage,
                    imageUrl = imageUrl
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
    onEdit: () -> Unit,
    imageUrl: String?
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
                // URL 있으면 네트워크 이미지, 없으면 기본 리소스
                val model: Any = if (imageUrl.isNullOrBlank()) {
                    R.drawable.basic_profile_image
                } else {
                    imageUrl
                }
                AsyncImage(
                    model = model,
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

