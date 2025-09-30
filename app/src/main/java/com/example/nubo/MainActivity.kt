package com.example.nubo


import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.nubo.data.repository.AuthRepository
import com.example.nubo.deeplink.DeepLinkContract
import com.example.nubo.deeplink.DeepLinkStore
import com.example.nubo.ui.screen.home.HomeScreen
import com.example.nubo.ui.screen.learn.LearnScreen
import com.example.nubo.ui.screen.myBoard.MyBoardScreen
import com.example.nubo.ui.component.BottomNavBar
import com.example.nubo.ui.component.sheet.BottomSheetHost
import com.example.nubo.ui.component.sheet.SheetRoute
import com.example.nubo.ui.screen.card.CardDetailRoute
import com.example.nubo.ui.screen.card.CardDetailViewModel
import com.example.nubo.ui.screen.editCard.EditCardRoute
import com.example.nubo.ui.screen.myBoard.BoardDetailScreen
import com.example.nubo.ui.screen.onBoardingLogin.OnBoardingLoginActivity
import com.example.nubo.ui.screen.profile.EditNameScreen
import com.example.nubo.ui.screen.profile.InformationScreen
import com.example.nubo.ui.screen.profile.NotificationScreen
import com.example.nubo.ui.screen.profile.ProfileRoute
import com.example.nubo.ui.theme.NuboAppTheme
import com.example.nubo.utils.cacheToStore
import com.example.nubo.utils.startOnboardingForLogin
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // Compose로 딥링크 Intent를 흘려보내는 파이프
    private val deepLinkEvents = MutableSharedFlow<Intent>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 시스템바 투명/아이콘 색
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        // 최초 진입 인텐트 캐시 + 송출
        intent?.let { cacheDeepLinkIfAny(it); deepLinkEvents.tryEmit(it) }

        setContent {
            NuboAppTheme {
                RequestNotificationPermissionOnce() // Android 13+ 권한
                MainScreen(deepLinkEvents = deepLinkEvents) // ⬅️ 전달
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        cacheDeepLinkIfAny(intent)
        deepLinkEvents.tryEmit(intent)
    }
}

private fun cacheDeepLinkIfAny(intent: Intent) {
    when (intent.getStringExtra(DeepLinkContract.EXTRA_DEEPLINK_TARGET)) {
        DeepLinkContract.TARGET_CARD_DETAIL -> {
            intent.getLongExtra(DeepLinkContract.EXTRA_CARD_ID, -1L)
                .takeIf { it > 0 }?.let { DeepLinkStore.pendingCardId = it }
        }

        DeepLinkContract.TARGET_CARD_UNREAD_LIST -> {
            DeepLinkStore.pendingGoUnread = true
        }

        DeepLinkContract.TARGET_BOARD_DETAIL,
        DeepLinkContract.TARGET_BOARD_INVITE -> {
            DeepLinkStore.pendingBoardId =
                intent.getLongExtra(DeepLinkContract.EXTRA_BOARD_ID, -1L).takeIf { it > 0 }
            DeepLinkStore.pendingBoardTitle =
                intent.getStringExtra(DeepLinkContract.EXTRA_BOARD_TITLE)
            DeepLinkStore.pendingInviteToken =
                intent.getStringExtra(DeepLinkContract.EXTRA_INVITE_TOKEN)
        }
    }
}

@Composable
fun RequestNotificationPermissionOnce() {
    if (Build.VERSION.SDK_INT < 33) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op */ }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    deepLinkEvents: SharedFlow<Intent>,
    vm: MainViewModel = hiltViewModel()
) {
    val isLoggedIn by vm.isLoggedIn.collectAsState()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 상세 화면에서는 BottomNavBar 숨기기
    val showBottomBar = currentRoute in listOf("home", "myboard", "add", "learn", "profile", "information","notification")
    var sheetRoute by remember { mutableStateOf<SheetRoute?>(null) }

    // Composable 컨텍스트에서 미리 Context를 받아둔다
    val context = LocalContext.current

    val contentInsets =
        WindowInsets.safeDrawing

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    selectedIndex = getSelectedIndex(currentRoute),
                    onItemSelected = { index ->
                        when (index) {
                            0 -> navController.navigate("home") { popUpTo("home"); launchSingleTop = true }
                            1 -> navController.navigate("myboard") { popUpTo("home"); launchSingleTop = true }
                            2 -> sheetRoute = SheetRoute.AddMenu
                            3 -> navController.navigate("learn") { popUpTo("home"); launchSingleTop = true }
                            4 -> navController.navigate("profile") { popUpTo("home"); launchSingleTop = true }
                        }
                    }, isLearnScreen = (currentRoute == "learn"), modifier = Modifier.navigationBarsPadding()
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("home") {
                HomeScreen(
                    onMoreClick = { navController.navigate("learn") },
                    onOpenBoard = { boardId, boardName ->
                        // Encode title for route
                        val encoded = URLEncoder.encode(boardName, StandardCharsets.UTF_8.toString())
                        navController.navigate("board_detail/$boardId/$encoded")
                    },
                    onOpenCardDetail = { id -> navController.navigate("card_detail/$id") },
                    modifier = Modifier
                        .padding(innerPadding)
                        .statusBarsPadding()
                )
            }
            composable("myboard") {
                MyBoardScreen(
                    navController,
                    modifier = Modifier
                        .padding(innerPadding)
                        .statusBarsPadding()
                )
            }
            composable("learn") {
                // learn 화면은 패딩을 적용하지 않음
                LearnScreen()
            }
            composable("profile") {
                // ViewModel과 묶인 Route로 교체
                ProfileRoute(
                    navController = navController,
                    onBack = { navController.popBackStack() },
                    onMyInfo = { navController.navigate("information") },
                    modifier = Modifier.padding(innerPadding),
                    onNotification = {navController.navigate("notification")}
                )
            }
            composable(
                route = "board_detail/{boardId}/{boardTitle}",
                arguments = listOf(
                    navArgument("boardId") { type = NavType.IntType },       // boardId is Int
                    navArgument("boardTitle") { type = NavType.StringType }   // title is String
                )
            ) { backStackEntry ->
                // Safe: types match the navArguments above
                val boardId = backStackEntry.arguments?.getInt("boardId") ?: return@composable
                val boardTitle = backStackEntry.arguments?.getString("boardTitle") ?: "로딩 중..."

                BoardDetailScreen(
                    boardId = boardId,
                    boardTitle = boardTitle,
                    navController = navController,
                    modifier = Modifier
                        .padding(innerPadding)
                        .statusBarsPadding()
                )
            }
            composable("information") {
                InformationScreen(
                    navController = navController,
                    onBack = { navController.popBackStack() }, // 뒤로가기
                    onEditProfileImage = { /* 편집 처리 */ },
                    onLogout = {
                        // 이미 InformationScreen에서 토큰/유저정보 삭제 후 이 콜백을 호출함
                        // 여기서는 온보딩 로그인 액티비티로 전환만 수행
                        val intent = Intent(context, OnBoardingLoginActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        context.startActivity(intent)
                    },
                    onWithdraw = { /* 탈퇴 처리 */ },
                    onEditName = { current -> navController.navigate("edit_name?initial=${Uri.encode(current)}") },
                )
            }
            composable("notification"){
                NotificationScreen(
                    navController = navController,
                    onBack = { navController.popBackStack() } // 뒤로가기
                )
            }
            composable(
                route = "edit_name?initial={initial}",
                arguments = listOf(navArgument("initial") { defaultValue = "" })
            ) { backStackEntry ->
                val initial = backStackEntry.arguments?.getString("initial").orEmpty()

                EditNameScreen(
                    initial = initial,
                    onBack = {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.remove<String>("edited_name")
                        navController.popBackStack()
                    },
                    onDone = { newName ->
                        // 값 반환 후 이전 화면으로
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("edited_name", newName)
                        navController.popBackStack()
                    },
                )
            }

            composable(
                route = "card_detail/{cardId}",
                arguments = listOf(navArgument("cardId") { type = NavType.IntType })
            ) { backStackEntry ->
                val cardId = backStackEntry.arguments?.getInt("cardId") ?: return@composable

                // 노트 수정 페이지에서 돌아올 경으 true로 세팅, 서버 재요청
                val refreshFlow = remember(backStackEntry) {
                    backStackEntry.savedStateHandle.getStateFlow("refresh_detail", false)
                }
                val refresh by refreshFlow.collectAsState(initial = false)

                // 카드 상세 VM을 현재 backStackEntry 스코프로 획득
                val detailVm: CardDetailViewModel = hiltViewModel(backStackEntry)

                // 최초 진입 시 로드 (VM에 load(cardId) 함수가 있다고 가정)
                LaunchedEffect(cardId) {
                    detailVm.refresh()
                }

                // 편집 완료 후 복귀 시 재요청
                LaunchedEffect(refresh) {
                    if (refresh) {
                        detailVm.refresh()                              // 서버에서 최신 상세 재조회
                        backStackEntry.savedStateHandle["refresh_detail"] = false // 플래그 초기화
                    }
                }

                CardDetailRoute(
                    onBack = { navController.popBackStack() },
                    onEdit = {
                        android.util.Log.d("Nav", "navigate -> card_edit/$cardId")
                        navController.navigate("card_edit/$cardId")
                    }
                )
            }

            composable(
                route = "card_edit/{cardId}",
                arguments = listOf(navArgument("cardId") { type = NavType.IntType })
            ) {
                EditCardRoute(
                    onBack = { navController.popBackStack() },
                    //저장 성공 시
                    onSaved = {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("refresh_detail", true)
                        navController.popBackStack()

                    }
                )
            }
        }
    }

    // ─────────────────────────────────────────────
    // 딥링크 처리: (1) 앱 준비 시 보관분, (2) 실시간 이벤트
    // ─────────────────────────────────────────────

    // (1) 앱 준비 후, 보관된 딥링크 1회 소비
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            // 로그인 안 되었으면 온보딩으로 전환하고, Store 값은 유지
            startOnboardingForLogin(context)
            return@LaunchedEffect
        }

        // 카드 상세
        DeepLinkStore.pendingCardId?.let { id ->
            DeepLinkStore.pendingCardId = null
            navController.navigate("card_detail/${id.toInt()}") {
                popUpTo("home") { inclusive = false }
                launchSingleTop = true
            }
        }

        // 미시청 카드 → learn
        if (DeepLinkStore.pendingGoUnread) {
            DeepLinkStore.pendingGoUnread = false
            navController.navigate("learn") {
                popUpTo("home") { inclusive = false }
                launchSingleTop = true
            }
        }

        // 보드 상세/초대
        DeepLinkStore.pendingBoardId?.let { bId ->
            val title = DeepLinkStore.pendingBoardTitle ?: "로딩 중..."
            DeepLinkStore.pendingBoardId = null
            DeepLinkStore.pendingBoardTitle = null

            val encoded = URLEncoder.encode(title, StandardCharsets.UTF_8.toString())
            navController.navigate("board_detail/${bId.toInt()}/$encoded") {
                popUpTo("home") { inclusive = false }
                launchSingleTop = true
            }
            // 초대 토큰: DeepLinkStore.pendingInviteToken 은 상세에서 소비
        }
    }

    // (2) 런타임 중 들어온 딥링크 처리
    LaunchedEffect(Unit) {
        deepLinkEvents.collectLatest { intent ->
            val target = intent.getStringExtra(DeepLinkContract.EXTRA_DEEPLINK_TARGET)

            if (!isLoggedIn) {
                // 미로그인: Store에 보관 후 온보딩으로
                cacheToStore(intent)
                startOnboardingForLogin(context)
                return@collectLatest
            }

            when (target) {
                DeepLinkContract.TARGET_CARD_DETAIL -> {
                    val id = intent.getLongExtra(DeepLinkContract.EXTRA_CARD_ID, -1L)
                    if (id > 0) {
                        navController.navigate("card_detail/${id.toInt()}") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }

                DeepLinkContract.TARGET_CARD_UNREAD_LIST -> {
                    navController.navigate("learn") {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                }

                DeepLinkContract.TARGET_BOARD_DETAIL,
                DeepLinkContract.TARGET_BOARD_INVITE -> {
                    val boardId = intent.getLongExtra(DeepLinkContract.EXTRA_BOARD_ID, -1L)
                    val title = intent.getStringExtra(DeepLinkContract.EXTRA_BOARD_TITLE) ?: "로딩 중..."
                    if (boardId > 0) {
                        val encoded = URLEncoder.encode(title, StandardCharsets.UTF_8.toString())
                        navController.navigate("board_detail/${boardId.toInt()}/$encoded") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                        DeepLinkStore.pendingInviteToken =
                            intent.getStringExtra(DeepLinkContract.EXTRA_INVITE_TOKEN)
                    }
                }
            }
        }
    }


    BottomSheetHost(
        route = sheetRoute,
        onDismiss = { sheetRoute = null },
        onGoCreateBoard = { sheetRoute = SheetRoute.CreateBoard },
        onGoInvite = { sheetRoute = SheetRoute.Invite },
        onCreateBoard = { name, isShared ->
            // TODO: call ViewModel to create board
            sheetRoute = null
            // Optional: navigate to new board detail
            // navController.navigate("board_detail/$newId/${Uri.encode(name)}")
        },
        onInvite = { email ->
            // TODO: invite logic via ViewModel
        },
        onGoAddVideo = { sheetRoute = SheetRoute.AddVideo },
        onBackToAddMenu = { sheetRoute = SheetRoute.AddMenu },
        onBackToCreateBoard = { sheetRoute = SheetRoute.CreateBoard },
        onInviteComplete = { emails ->
            // TODO: 서버 전달 (예: viewModel.inviteMembers(boardId, emails))
        }
    )
}


fun getSelectedIndex(route: String?): Int {
    return when (route) {
        "home" -> 0
        "myboard" -> 1
        "add" -> 2
        "learn" -> 3
        "profile", "information" -> 4
        else -> -1
    }
}
