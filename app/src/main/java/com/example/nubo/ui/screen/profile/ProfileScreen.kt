package com.example.nubo.ui.screen.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.components.toast.AppToastLayout
import com.example.components.toast.AppToastType
import com.example.components.toast.LocalAppToastHostState
import com.example.nubo.R
import com.example.nubo.ui.component.noRippleClickable
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey10
import com.example.nubo.ui.theme.Purple300

// Route: ViewModel과 상태 바인딩
@Composable
fun ProfileRoute(
    navController: NavController,
    onBack: () -> Unit = {},
    onBellClick: () -> Unit = {},
    onEditProfileImage: () -> Unit = {},
    onMyInfo: () -> Unit = {},
    onNotification: () -> Unit = {},
    onHelp: () -> Unit = {},
    onPrivacy: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    // InformationScreen에서 넘어온 override 값 구독
    val backStackEntry = navController.currentBackStackEntry
    val overrideFlow = backStackEntry?.savedStateHandle
        ?.getStateFlow("profile_name_override", "")
    val override by (overrideFlow?.collectAsState() ?: remember { mutableStateOf("") })

    LaunchedEffect(override) {
        if (override.isNotBlank()) {
            viewModel.applyLocalName(override)
            backStackEntry?.savedStateHandle?.remove<String>("profile_name_override")
        }
    }

    // 서버에서 가져온 프로필 UI 상태 구독
    val state by viewModel.uiState.collectAsState()

    // 화면 최초 진입 시 unread 알림 여부 조회
    LaunchedEffect(Unit) {
        viewModel.loadUnreadNotificationState()
    }

    // 전역 토스트 호스트
    val toastHostState = LocalAppToastHostState.current

    when {
        // 로딩 상태
        state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        // 에러 상태
        state.error != null -> {
            // 에러가 발생했을 때 한 번만 토스트 출력
            // 화면 진입 시 커스텀 토스트 호출 (1회성)
//            LaunchedEffect(Unit) {
//                toastHostState.show(
//                    title = AnnotatedString("정보를 불러오지 못했어요"),
//                    summary = "네트워크 확인 후 다시 시도해주세요.", // 두 번째 줄은 summary로 처리
//                    type = AppToastType.NEGATIVE,         // 에러 아이콘 타입
//                    layout = AppToastLayout.TitleWithSummary // 제목 + 설명 레이아웃
//                )
//            }

            // 실패해도 기본값으로 UI는 계속 표시
            ProfileScreen(
                nickname = "이름 없음",
                email = "이메일 없음",
                profileImageUrl = null,
                onBack = onBack,
                onBellClick = onBellClick,
                onEditProfileImage = onEditProfileImage,
                onMyInfo = onMyInfo,
                onNotification = onNotification,
                onHelp = onHelp,
                onPrivacy = onPrivacy,
                hasUnreadNotification = false
            )
        }

        // 성공 상태
        else -> {
            val name = state.data?.name?.takeIf { !it.isNullOrBlank() } ?: "이름 없음"
            val email = state.data?.email?.takeIf { !it.isNullOrBlank() } ?: "이메일 없음"
            val imageUrl = state.data?.profileImageUrl

            ProfileScreen(
                nickname = name,
                email = email,
                profileImageUrl = imageUrl,
                onBack = onBack,
                onBellClick = onBellClick,
                onEditProfileImage = onEditProfileImage,
                onMyInfo = onMyInfo,
                onNotification = onNotification,
                onHelp = onHelp,
                onPrivacy = onPrivacy,
                hasUnreadNotification = state.hasUnreadNotification
            )
        }
    }
}

//실제 화면 UI: 서버 값 전달받아 렌더링
@Composable
fun ProfileScreen(
    nickname: String,
    email: String,
    profileImageUrl: String?,
    onBack: () -> Unit = {},
    onBellClick: () -> Unit = {},
    onEditProfileImage: () -> Unit = {},
    onMyInfo: () -> Unit = {},
    onNotification: () -> Unit = {},
    onHelp: () -> Unit = {},
    onPrivacy: () -> Unit = {},
    hasUnreadNotification: Boolean = false
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
            // 상태바 안전영역 안의 아이콘(타이틀/알림)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 왼쪽 영역 비워두기 (Spacer로 공간 확보)
                Spacer(modifier = Modifier.size(38.dp))

                // 가운데 텍스트
                Text(
                    text = "마이페이지",
                    style = AppTextStyles.subtitle_semibold_20,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 알림 버튼
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.Black.copy(alpha = 0.05f), CircleShape)
                        .noRippleClickable(onClick = onBellClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (hasUnreadNotification) R.drawable.bell_noti else R.drawable.bell
                        ),
                        contentDescription = "알림",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(26.dp)
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
                    purple = MaterialTheme.colorScheme.primary,
                    imageUrl = profileImageUrl // ← 추가
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
                    modifier = Modifier.padding(start = 32.dp, top = 8.dp, bottom = 15.dp)
                )

                SettingsItem(title = "내 정보", onClick = onMyInfo)
                SettingsItem(title = "알림", onClick = onNotification)

                Spacer(Modifier.height(15.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(Grey10)   // 원하는 배경색
                )

                Spacer(Modifier.height(15.dp))

                SettingsItem(title = "도움말", onClick = onHㅌelp)
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
    onEdit: () -> Unit,
    imageUrl: String?
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
                val model: Any = if (imageUrl.isNullOrBlank()) {
                    R.drawable.basic_profile_image
                } else {
                    imageUrl
                }
                AsyncImage(
                    model = model,
                    contentDescription = "프로필",
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
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
            .noRippleClickable(onClick = onClick)
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
