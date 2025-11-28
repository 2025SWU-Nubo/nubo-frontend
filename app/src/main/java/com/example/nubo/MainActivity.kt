package com.example.nubo

import android.Manifest
import android.content.Intent
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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.components.toast.AppToastLayout
import com.example.components.toast.AppToastOverlay
import com.example.components.toast.AppToastType
import com.example.components.toast.rememberAppToastHostState
import com.example.nubo.deeplink.DeepLinkContract
import com.example.nubo.deeplink.DeepLinkStore
import com.example.nubo.ui.component.BottomNavBar
import com.example.nubo.ui.component.sheet.BottomSheetHost
import com.example.nubo.ui.component.sheet.CreateBoardViewModel
import com.example.nubo.ui.component.sheet.SheetRoute
import com.example.nubo.ui.screen.card.CardDetailRoute
import com.example.nubo.ui.screen.card.CardDetailViewModel
import com.example.nubo.ui.screen.cardupload.CardUploadViewModel
import com.example.nubo.ui.screen.editCard.EditCardRoute
import com.example.nubo.ui.screen.home.HomeScreen
import com.example.nubo.ui.screen.home.HomeViewModel
import com.example.nubo.ui.screen.interest.OnBoardingInterestRoute
import com.example.nubo.ui.screen.learn.LearnScreen
import com.example.nubo.ui.screen.myBoard.ActionsContent
import com.example.nubo.ui.screen.myBoard.BoardAction
import com.example.nubo.ui.screen.myBoard.BoardDetailScreen
import com.example.nubo.ui.screen.myBoard.BoardDetailViewModel
import com.example.nubo.ui.screen.myBoard.BoardSelectionSheetContent
import com.example.nubo.ui.screen.myBoard.DeleteConfirmationDialog
import com.example.nubo.ui.screen.myBoard.MyBoardRoute
import com.example.nubo.ui.screen.myBoard.MyBoardScreen
import com.example.nubo.ui.screen.notification.NotiEvent
import com.example.nubo.ui.screen.myBoard.SectionDetailScreen
import com.example.nubo.ui.screen.myBoard.SelectionBottomBar
import com.example.nubo.ui.screen.notification.NotificationScreen
import com.example.nubo.ui.screen.notification.NotificationViewModel
import com.example.nubo.ui.screen.onBoardingLogin.OnBoardingLoginActivity
import com.example.nubo.ui.screen.profile.EditNameScreen
import com.example.nubo.ui.screen.profile.InformationScreen
import com.example.nubo.ui.screen.profile.NotificationSetScreen
import com.example.nubo.ui.screen.profile.ProfileRoute
import com.example.nubo.ui.theme.NuboAppTheme
import com.example.nubo.utils.cacheToStore
import com.example.nubo.utils.postRefreshTick
import com.example.nubo.utils.startOnboardingForLogin
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import com.example.components.toast.LocalAppToastHostState
import com.example.nubo.ui.component.toast.GlobalToastBus
import com.example.nubo.ui.screen.learn.LearnScreenBerry
import com.example.nubo.ui.screen.onBoadingTutorial.OnBoardingTutorialRoute
import com.example.nubo.ui.screen.recommendCard.RecommendCardDetailScreen
import com.example.nubo.ui.screen.recommendCard.RecommendCardDetailViewModel
import com.example.nubo.ui.screen.recommendCard.RecommendDetailUiState
import com.example.nubo.ui.screen.recommendCard.SaveUiState
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey500
import com.example.nubo.ui.theme.GreyMain300

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // Pipe to deliver deep link intents into Compose world
    private val deepLinkEvents = MutableSharedFlow<Intent>(extraBufferCapacity = 1)

    companion object{
        private const val  TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        android.util.Log.d(TAG, "onCreate()")
        android.util.Log.d(TAG, "onCreate() intent=${intent?.action} extras=${intent?.extras?.keySet()?.joinToString()}")


        // Make system bars transparent and set icon appearance
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        // 초기 인텐트 캐시 & emit
        intent?.let {
            android.util.Log.d(TAG, "onCreate() cacheDeepLinkIfAny() with initial intent")
            cacheDeepLinkIfAny(it);
            deepLinkEvents.tryEmit(it)
        }

        setContent {
            NuboAppTheme {
                // 1  전역 토스트 호스트를 Activity 루트에서 remember
                val toastHost = rememberAppToastHostState()


                // 2  CompositionLocal로 전체 앱에 제공
                CompositionLocalProvider(LocalAppToastHostState provides toastHost) {
                    RequestNotificationPermissionOnce()

                    // 3  MainScreen 위에 전역 토스트 오버레이를 항상 깔아둠
                    Box(Modifier.fillMaxSize()) {
                        MainScreen(deepLinkEvents = deepLinkEvents)
                        AppToastOverlay(
                            hostState = toastHost,
                            extraBottomOffset = 0.dp
                        )
                    }
                }
            }
        }

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        android.util.Log.d(TAG,"onNewIntent() action=${intent.action} extras=${intent.extras?.keySet()?.joinToString()}")
        cacheDeepLinkIfAny(intent)
        deepLinkEvents.tryEmit(intent)
    }
}

// 인텐트에 담긴 딥링크/FCM 정보를 DeepLinkStore에 저장
private fun cacheDeepLinkIfAny(intent: Intent) {

    android.util.Log.d("MainActivity", "cacheDeepLinkIfAny(): EXTRA_DEEPLINK_TARGET=${intent.getStringExtra(com.example.nubo.deeplink.DeepLinkContract.EXTRA_DEEPLINK_TARGET)} type=${intent.getStringExtra("type")} cardId=${intent.getStringExtra("cardId")} boardId=${intent.getStringExtra("boardId")}")
    // 기존 DeepLinkContract 포맷과 FCM 데이터 페이로드를 모두 처리하도록 유틸을 사용
    cacheToStore(intent)
    // 캐시 결과도 찍기
    android.util.Log.d("MainActivity", "DeepLinkStore: cardId=${com.example.nubo.deeplink.DeepLinkStore.pendingCardId}, goUnread=${com.example.nubo.deeplink.DeepLinkStore.pendingGoUnread}, openNoti=${com.example.nubo.deeplink.DeepLinkStore.pendingOpenNotificationCenter}, boardId=${com.example.nubo.deeplink.DeepLinkStore.pendingBoardId}, boardTitle=${com.example.nubo.deeplink.DeepLinkStore.pendingBoardTitle}")
}


@Composable
fun RequestNotificationPermissionOnce() {
    if (Build.VERSION.SDK_INT < 33) return
    val context = androidx.compose.ui.platform.LocalContext.current
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
    val context = androidx.compose.ui.platform.LocalContext.current

    // 전역 CompositionLocal 에서 토스트 호스트 가져오기
    val toastHost = LocalAppToastHostState.current
    val toastScope = rememberCoroutineScope()

    // message, type, duration, preDelay, actionLabel, onAction
    fun showToast(
        msg: String,
        type: AppToastType,
        duration: Int = 2000,
        preDelay: Int = 0,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        toastScope.launch {
            toastHost.show(
                title = AnnotatedString(msg),
                type = type,
                layout = if (actionLabel != null && onAction != null) {
                    AppToastLayout.TitleWithAction
                } else {
                    AppToastLayout.TitleOnly
                },
                durationMillis = duration,
                preDelayMillis = preDelay,
                actionLabel = actionLabel,
                onAction = onAction
            )
        }
    }

    val cardUploadVm: CardUploadViewModel = hiltViewModel()

    // 보드 생성을 감지
    val createBoardViewModel: CreateBoardViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        cardUploadVm.uploadEvents.collect { ev ->

            delay(180)

            when (ev) {
                // Started는 토스트 안 띄우고, 각 화면에서 로딩 UI로만 처리
                CardUploadViewModel.UploadEvent.Started -> Unit

                CardUploadViewModel.UploadEvent.Succeeded -> {
                    navController.postRefreshTick("myboard")
                    showToast(
                        msg = "카드 생성을 완료했어요!",
                        type = AppToastType.POSITIVE,
                        duration = 1500,
                        preDelay = 800
                    )
                }

                CardUploadViewModel.UploadEvent.AlreadyExists -> {
                    showToast(
                        msg = "이미 추가된 영상이에요",
                        type = AppToastType.NEGATIVE,
                        duration = 1500,
                        preDelay = 800
                    )
                }

                is CardUploadViewModel.UploadEvent.Failed -> {
                    showToast(
                        msg = ev.message,
                        type = AppToastType.NEGATIVE,
                        duration = 2000,
                        preDelay = 800
                    )
                }
            }

            // Succeeded/AlreadyExists이면 home refresh 신호
            if (ev is CardUploadViewModel.UploadEvent.Succeeded ||
                ev is CardUploadViewModel.UploadEvent.AlreadyExists
            ) {
                navController.getBackStackEntry("home")
                    .savedStateHandle["refresh_home"] = System.currentTimeMillis()
            }
        }
    }

    // CreateBoardViewModel의 UI 상태를 구독
    val createBoardState by createBoardViewModel.ui.collectAsStateWithLifecycle()

    // 보드 생성 완료 시
    LaunchedEffect(createBoardState.created) {
        // 'created' 상태가 null이 아니면, 보드 생성이 성공한 것
        if (createBoardState.created != null) {
            // MyBoardRoute가 감지할 수 있도록 "needs_refresh" 플래그 설정
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.set("needs_refresh", true)
            android.util.Log.d("Myboard","새로고침 신호 전송!")

            // 플래그를 재설정하여 중복 새로고침 방지
//            createBoardViewModel.consumeCreated()
        }
    }

    LaunchedEffect(isLoggedIn) {
        android.util.Log.d("MainActivity","isLoggendIn changed: $isLoggedIn")
        if(!isLoggedIn){
            android.util.Log.d("MainActivity","navigate -> OnBoarding(not loggin in)")
            startOnboardingForLogin(context)
            return@LaunchedEffect
        }
    }

    // Listen global toast bus (for Service etc.) and show via host
    LaunchedEffect(Unit) {
        GlobalToastBus.events.collectLatest { event ->
            toastHost.show(
                title = AnnotatedString(event.message),
                type = event.type,
                layout = event.layout,
                durationMillis = event.durationMillis,
                preDelayMillis = event.preDelayMillis,
                actionLabel = event.actionLabel,
                onAction = event.onAction
            )
        }
    }



    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isEditScreen = currentRoute?.startsWith("card_edit") == true
    val isCardDetail = currentRoute?.startsWith("card_detail") == true

    // MyBoardScreen이 선택 모드일 때, Main의 하단 바를 숨기기 위한 상태
    var isMyBoardSelectionMode by remember { mutableStateOf(false) }

    // Hide BottomNav on detail-like screens
    val showBottomBar = currentRoute in listOf(
        "home", "myboard", "add", "learn", "profile", "information"
    )

    var sheetRoute by remember { mutableStateOf<SheetRoute?>(null) }
//    val context = androidx.compose.ui.platform.LocalContext.current
    val contentInsets = WindowInsets.safeDrawing

    Scaffold(
        // 특정 화면일 때만 시스템 인셋 자동패딩 제거
        contentWindowInsets = if (isCardDetail || isEditScreen || currentRoute == "profile" || currentRoute == "information" ||
            currentRoute == "edit_name?initial={initial}" || currentRoute == "learn" || currentRoute == "notification"
        ) {
            WindowInsets(0)
        } else {
            ScaffoldDefaults.contentWindowInsets
        },
        bottomBar = {
            // showBottomBar가 true이고, MyBoard가 선택모드가 아닐 때만 BottomNavBar를 보여줌
            if (showBottomBar && !isMyBoardSelectionMode) {
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
                        },
                        isLearnScreen = (currentRoute == "learn"),
                        modifier = Modifier.navigationBarsPadding()
                    )
                }
        }
    ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.fillMaxSize()
            ) {
                    // ① 튜토리얼
                    composable(
                        route = "onboarding_tutorial/{needsInterest}",
                        arguments = listOf(
                            navArgument("needsInterest") { type = NavType.BoolType }
                        )
                    ) { backStackEntry ->
                        val needsInterestArg =
                            backStackEntry.arguments?.getBoolean("needsInterest") ?: false

                        OnBoardingTutorialRoute(
                            needsInterest = needsInterestArg,
                            onGoInterest = {
                                // 튜토리얼 끝나고 관심사 설정으로
                                navController.navigate("onboarding_interest") {
                                    // 온보딩 스택은 정리
                                    popUpTo(navController.graph.id) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onGoHome = {
                                // 튜토리얼 끝났고 관심사도 필요 없으면 바로 홈
                                navController.navigate("home") {
                                    popUpTo(navController.graph.id) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    // ② 기존 관심사 설정 라우트
                    composable("onboarding_interest") {
                        OnBoardingInterestRoute(
                            onBack = { navController.popBackStack() },
                            onHome = {
                                navController.navigate("home") {
                                    popUpTo(navController.graph.id) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            thumbnailsRes = mapOf(
                                59L to R.drawable.interest_education,
                                60L to R.drawable.interest_education,
                                61L to R.drawable.interest_education
                            )
                        )
                    }


                composable("home") { backStackEntry ->
                    val homeVm: HomeViewModel = hiltViewModel(backStackEntry)

                    // Main에서 쏜 신호를 StateFlow로 구독
                    val refreshFlow = remember(backStackEntry) {
                        backStackEntry.savedStateHandle.getStateFlow("refresh_home", 0L)
                    }

                    // 신호가 오면 즉각 홈 데이터 갱신
                    LaunchedEffect(Unit) {
                        refreshFlow
                            .drop(1) // 초기값 0L은 무시
                            .collectLatest {
                                // 현재 칩 선택 유지한 채 카드 새로고침 + 최근 본 보드도 갱신
                                homeVm.refreshForCurrentSelection()
                                homeVm.loadRecentBoards()
                            }
                    }

                    HomeScreen(
                        modifier = Modifier
                            .padding(innerPadding)
                            .statusBarsPadding(),
                        onMoreClick = { navController.navigate("learn") },
                        onOpenBoard = { boardId, boardName ->
                            val encoded = URLEncoder.encode(
                                boardName,
                                StandardCharsets.UTF_8.toString()
                            )
                            navController.navigate("board_detail/$boardId/$encoded")
                        },
                        onOpenCardDetail = { id -> navController.navigate("card_detail/$id") },
                        // 추천 카드 상세
                        onOpenRecommendCard = { id ->
                            navController.navigate("recommend_card_detail/$id")
                        },
                        onNotificationsClick = {
                            navController.navigate("notification") {
                                launchSingleTop = true
                                popUpTo("home") { inclusive = false }
                            }
                        },
                    )
                }

                composable("myboard") {
                    // MyBoardRoute를 호출
                    MyBoardRoute(
                        navController = navController,
                        modifier = Modifier
                            .padding(innerPadding)
                            .statusBarsPadding(),
                        onSelectionModeChange = { inSelectionMode ->
                            isMyBoardSelectionMode = inSelectionMode
                        }
                    )
                }

                composable("learn") {
                    // Learn keeps its own insets strategy
                    LearnScreen(navController = navController) // navController를 전달합니다.
                }

                composable(
                    route = "learnBerry/{berryCount}",
                    arguments = listOf(
                        navArgument("berryCount") { type = NavType.IntType }
                    )
                ) { backStackEntry ->
                    val berryCountArg = backStackEntry.arguments?.getInt("berryCount") ?: 0

                    LearnScreenBerry(
                        berryCount = berryCountArg,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable("profile") {
                    ProfileRoute(
                        navController = navController,
                        onBellClick ={navController.navigate("notification")},
                        onBack = { navController.popBackStack() },
                        onMyInfo = { navController.navigate("information") },
                        modifier = Modifier.padding(innerPadding),
                        onNotification = { navController.navigate("notificationSet") }
                    )
                }

                composable(
                    route = "board_detail/{boardId}/{boardTitle}?source={source}",
                    arguments = listOf(
                        navArgument("boardId") { type = NavType.IntType },
                        navArgument("boardTitle") { type = NavType.StringType },
                        navArgument("source") { defaultValue = "USER"; nullable = true }
                    )
                ) { backStackEntry ->
                    val boardId = backStackEntry.arguments?.getInt("boardId") ?: return@composable
                    val boardTitle = backStackEntry.arguments?.getString("boardTitle") ?: "로딩 중..."
                    // source 값을 backStackEntry에서 추출
                    val source = backStackEntry.arguments?.getString("source") ?: "USER" // 기본값을 "USER"로 설정
                    BoardDetailScreen(
                        boardId = boardId,
                        boardTitle = boardTitle,
                        source = source,
                        navController = navController,
                        modifier = Modifier.statusBarsPadding()
                    )
                }

                composable(
                    route = "section_detail/{sectionId}/{sectionTitle}/{boardTitle}",
                    arguments = listOf(
                        navArgument("sectionId") { type = NavType.IntType },
                        navArgument("sectionTitle") { type = NavType.StringType },
                        navArgument("boardTitle") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val sectionId = backStackEntry.arguments?.getInt("sectionId") ?: return@composable
                    val sectionTitle = backStackEntry.arguments?.getString("sectionTitle") ?: "로딩 중..."
                    val boardTitle = backStackEntry.arguments?.getString("boardTitle") ?: ""
                    SectionDetailScreen(
                        sectionId = sectionId,
                        sectionTitle = sectionTitle,
                        navController = navController,
                        boardTitle = boardTitle
                    )
                }

                composable("information") {
                    InformationScreen(
                        navController = navController,
                        onBack = { navController.popBackStack() },
                        onEditProfileImage = { /* no-op */ },
                        onLogout = {
                            // InformationScreen already clears tokens then calls this
                            val intent = Intent(context, OnBoardingLoginActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            }
                            context.startActivity(intent)
                        },
                        onWithdraw = { /* no-op */ },
                        onEditName = { current ->
                            navController.navigate("edit_name?initial=${java.net.URLEncoder.encode(current, "UTF-8")}")
                        },
                        modifier = Modifier
                            .padding(innerPadding)
                            .statusBarsPadding()
                    )
                }
                composable("notificationSet"){
                    NotificationSetScreen(
                        onBack = { navController.popBackStack() }
                    )

                }

                // Notification page unified to use ViewModel version
                composable("notification") {
                    val nvm: NotificationViewModel = hiltViewModel()
                    val uiState by nvm.uiState.collectAsState()

                    NotificationScreen(
                        state = uiState,
                        onRefresh = { nvm.refresh() },
                        onBack = { navController.popBackStack() },
                        onAlarmSetting = { navController.navigate("notificationSet") },
                        onClickItem = { item -> nvm.onClickItem(item) },
                        onAcceptInvite = { item -> nvm.onClickPrimary(item) },
                        onRejectInvite = { item -> nvm.onClickSecondary(item) },
                        onShowMore = { _ -> nvm.onClickMore() },
                        onMarkAllRead = { nvm.onClickMarkAllRead() }
                    )

                    // 2) 단발 이벤트 수신 → 실제 네비게이션 수행
                    LaunchedEffect(Unit) {
                        nvm.events.collectLatest { e ->
                            when (e) {
                                is NotiEvent.GoCardDetail -> {
                                    // route: card_detail/{cardId}
                                    e.cardId.toIntOrNull()?.takeIf { it > 0 }?.let { id ->
                                        navController.navigate("card_detail/$id") {
                                            popUpTo("home") { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    }
                                }
                                NotiEvent.GoLearn -> {
                                    navController.navigate("myboard") {
                                        popUpTo("home") { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                                NotiEvent.GoNotificationCenter -> {
                                    // 이미 알림 화면에 있으므로 필요 시 no-op 또는 스낵바 등
                                }
                                is NotiEvent.GoBoard -> {
                                    // 필요 시 보드 상세 라우팅 규격에 맞춰 이동
                                    e.boardId.toIntOrNull()?.let { bId ->
                                        // title이 필요하면 VM에서 같이 싣거나 별도 조회
                                        navController.navigate("board_detail/$bId/${URLEncoder.encode("보드", "UTF-8")}") {
                                            popUpTo("home") { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            }
                        }
                    }

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
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("edited_name", newName)
                            navController.popBackStack()
                        },
                        modifier = Modifier
                            .padding(innerPadding)
                            .statusBarsPadding()
                    )
                }

                composable(
                    route = "card_detail/{cardId}",
                    arguments = listOf(navArgument("cardId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val cardId = backStackEntry.arguments?.getInt("cardId") ?: return@composable

                    // Observe refresh flag from edit page
                    val refreshFlow = remember(backStackEntry) {
                        backStackEntry.savedStateHandle.getStateFlow("refresh_detail", false)
                    }
//                    val refresh by refreshFlow.collectAsState(initial = false)

                    // Obtain VM scoped to this backStackEntry
                    val detailVm: CardDetailViewModel = hiltViewModel(backStackEntry)

                    // Initial load
                    LaunchedEffect(cardId) { detailVm.refresh() }

                    // Reload after edit
                    LaunchedEffect(Unit) {
                        refreshFlow
                            .drop(1)                 // ignore initial false
                            .distinctUntilChanged()  // avoid duplicate same values
                            .collect { shouldRefresh ->
                                if (shouldRefresh) {
                                    detailVm.refresh()
                                    backStackEntry.savedStateHandle["refresh_detail"] = false
                                }
                            }
                    }

                    Box(Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                    ) {
                        CardDetailRoute(
                            navController = navController,
                            onBack = { navController.popBackStack() },
                            onEdit = { navController.navigate("card_edit/$cardId") }
                        )
                    }
                }

                composable(
                    route = "card_edit/{cardId}",
                    arguments = listOf(navArgument("cardId") { type = NavType.IntType })
                ) {
                    Box(Modifier
                        .fillMaxSize()
//                        .padding(top = 50.dp)
                        .padding(innerPadding)
//                        .statusBarsPadding()
                    ) {
                        EditCardRoute(
                            navController = navController,
                            onBack = { navController.popBackStack() },
                            onSaved = {
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("refresh_detail", true)
                                navController.popBackStack()
                            }
                        )
                    }
                }

                composable(
                    route = "recommend_card_detail/{cardId}",
                    arguments = listOf(navArgument("cardId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val vm: RecommendCardDetailViewModel = hiltViewModel(backStackEntry)
                    val uiState by vm.uiState.collectAsState()
                    val saveState by vm.saveState.collectAsState()

                    // 처음 진입 시 상세 로드
                    LaunchedEffect(Unit) {
                        vm.load()
                    }

                    // 저장 결과 처리
                    LaunchedEffect(saveState) {
                        when (val s = saveState) {
                            is SaveUiState.Success -> {
                                // 응답으로 받은 cardId 로 일반 카드 상세로 이동
                                navController.navigate("card_detail/${s.cardId}") {
                                    popUpTo("home") { inclusive = false }
                                    launchSingleTop = true
                                }
                                showToast(
                                    "내 카드에 저장했어요",
                                    AppToastType.POSITIVE,
                                    duration = 1800,
                                    preDelay = 600,          // 여기서 전체 지연 조절
                                    actionLabel = null,
                                    onAction = null
                                )
                            }

                            is SaveUiState.Error -> {
                                showToast(
                                    s.message,
                                    AppToastType.NEGATIVE,
                                    duration = 2200,
                                    preDelay = 150,
                                    actionLabel = null,
                                    onAction = null
                                )
                            }

                            else -> Unit
                        }
                    }

                    when (val s = uiState) {
                        RecommendDetailUiState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is RecommendDetailUiState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = s.message,
                                    style = AppTextStyles.b2_regular_16,
                                    color = Grey500,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        is RecommendDetailUiState.Success -> {
                            RecommendCardDetailScreen(
                                item = s.item,
                                onBack = { navController.popBackStack() },
                                onSaveClick = { vm.saveToMyCards() }
                            )
                        }
                    }
                }

            }
    }

    // 로그인 상태가 결정된 직후, 캐시된 딥링크를 소진하여 이동
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            startOnboardingForLogin(context)
            return@LaunchedEffect
        }

        val activity = context as? MainActivity
        val intent = activity?.intent

        // 새로 추가할 플래그  OnBoardingViewModel 쪽에서 세팅해주면 됨
        val needsTutorial = intent?.getBooleanExtra("EXTRA_NEEDS_TUTORIAL", false) ?: false
        val needsInterest = intent?.getBooleanExtra("EXTRA_NEEDS_INTEREST", false) ?: false

        // 한 번만 쓰도록 제거
        intent?.removeExtra("EXTRA_NEEDS_TUTORIAL")
        intent?.removeExtra("EXTRA_NEEDS_INTEREST")

        when {
            needsTutorial -> {
                // 튜토리얼부터 보여주고, 끝나면 관심사 필요 여부를 같이 넘김
                navController.navigate("onboarding_tutorial/$needsInterest") {
                    popUpTo("home") { inclusive = true }
                    launchSingleTop = true
                }
                return@LaunchedEffect
            }
            needsInterest -> {
                // 튜토리얼은 볼 필요 없고, 관심사만 설정해야 하는 경우
                navController.navigate("onboarding_interest") {
                    popUpTo("home") { inclusive = true }
                    launchSingleTop = true
                }
                return@LaunchedEffect
            }
            else -> {
                // 둘 다 필요 없으면 기존 딥링크 처리 로직 그대로
            }
        }

        DeepLinkStore.pendingCardId?.let { raw ->
            DeepLinkStore.pendingCardId = null
            raw.toLongOrNull()?.takeIf { it > 0L }?.let { idLong ->
                navController.navigate("card_detail/${idLong.toInt()}") {
                    popUpTo("home") { inclusive = false }
                    launchSingleTop = true
                }
            }
        }

        // 미시청 목록(학습 탭) 진입 플래그가 있으면 이동함
        if (DeepLinkStore.pendingGoUnread) {
            DeepLinkStore.pendingGoUnread = false
            navController.navigate("myboard") {
                popUpTo("home") { inclusive = false }
                launchSingleTop = true
            }
        }

        // 보드 상세/초대에 대한 캐시가 있으면 이동
        DeepLinkStore.pendingBoardId?.let { bId ->
            val title = DeepLinkStore.pendingBoardTitle ?: "로딩 중..."
            DeepLinkStore.pendingBoardId = null
            DeepLinkStore.pendingBoardTitle = null

            val encoded = URLEncoder.encode(title, StandardCharsets.UTF_8.toString())
            navController.navigate("board_detail/${bId.toInt()}/$encoded") {
                popUpTo("home") { inclusive = false }
                launchSingleTop = true
            }
            // pendingInviteToken은 상세 화면에서 필요 시 사용
        }
    }

    // 앱 실행 중 수신되는 딥링크/FCM 인텐트를 실시간으로 처리함
    LaunchedEffect(Unit) {
        deepLinkEvents.collectLatest { intent ->
            val target = intent.getStringExtra(DeepLinkContract.EXTRA_DEEPLINK_TARGET)

            if (!isLoggedIn) {
                cacheToStore(intent)
                startOnboardingForLogin(context)
                return@collectLatest
            }

            // 1) 기존 DeepLinkContract 타깃이 있으면 먼저 처리함
            when (target) {
                DeepLinkContract.TARGET_CARD_DETAIL -> {
                    val id = intent.getLongExtra(DeepLinkContract.EXTRA_CARD_ID, -1L)
                    if (id > 0) {
                        navController.navigate("card_detail/${id.toInt()}") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                    return@collectLatest // 기존 포맷을 처리했으므로 조기 반환함
                }
                DeepLinkContract.TARGET_CARD_UNREAD_LIST -> {
                    navController.navigate("notification") {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                    return@collectLatest // 한 줄 주석: 기존 포맷 처리 완료
                }
                // 보드 상세는 보드로 이동
                DeepLinkContract.TARGET_BOARD_DETAIL -> {
                    val boardId = intent.getLongExtra(DeepLinkContract.EXTRA_BOARD_ID, -1L)
                    val title = intent.getStringExtra(DeepLinkContract.EXTRA_BOARD_TITLE) ?: "로딩 중..."
                    if (boardId > 0) {
                        val encoded = java.net.URLEncoder.encode(title, java.nio.charset.StandardCharsets.UTF_8.toString())
                        navController.navigate("board_detail/${boardId.toInt()}/$encoded") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                    return@collectLatest
                }
                // 초대 알림은 알림센터로 이동(수락/거절 UX)
                DeepLinkContract.TARGET_BOARD_INVITE -> {
                    navController.navigate("notification") {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                    return@collectLatest
                }
            }

            //  2) DeepLinkContract가 없다면 FCM 데이터 페이로드(type)를 해석함
            when (intent.getStringExtra("type")) {
                // 카드 생성/리마인드는 카드 상세로 이동함
                // (교체) deepLinkEvents.collectLatest { intent -> ... } 블록의 CARD_ADDED 처리
                "CARD_ADDED" -> {
                    // 우선 인텐트에서 문자열을 받되, 없으면 캐시를 이용
                    val raw = intent.getStringExtra("card_id")
                        ?: intent.getStringExtra("cardId")
                        ?: DeepLinkStore.pendingCardId
                    DeepLinkStore.pendingCardId = null

                    raw?.toLongOrNull()?.takeIf { it > 0L }?.let { idLong ->
                        navController.navigate("card_detail/${idLong.toInt()}") {
                            popUpTo("notification") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }

                "REMINDER" -> {
                    navController.navigate("home") {
                        popUpTo("notification") { inclusive = false }
                        launchSingleTop = true
                    }
                }
                "BOARD" -> {
                    navController.navigate("notification") {
                        popUpTo("notification") { inclusive = false }
                        launchSingleTop = true
                    }
                }

                // 레거시 문자열도 계속 지원
                "CARD_CREATED", "UNREAD_RECOMMEND" -> {
                    val id = intent.getStringExtra("cardId")?.toIntOrNull()
                    if (id != null && id > 0) {
                        navController.navigate("card_detail/$id") {
                            popUpTo("notification") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
                "BOARD_INVITE", "INVITE_RESULT" -> {
                    navController.navigate("notification") {
                        popUpTo("notification") { inclusive = false }
                        launchSingleTop = true
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
        onCreateBoard = { _, _ ->
            // Board creation callback from sheet  refresh list, close sheet etc
            sheetRoute = null
        },
        onInvite = { email ->
            // TODO Invite via ViewModel
        },
        onGoAddVideo = { sheetRoute = SheetRoute.AddVideo },
        onBackToAddMenu = { sheetRoute = SheetRoute.AddMenu },
        onBackToCreateBoard = { sheetRoute = SheetRoute.CreateBoard },
        onInviteComplete = { emails ->
            // TODO Submit invites via ViewModel
        },
        onClickCreatedBoard = { boardId, boardName ->
            // Navigate to created board detail when toast action is clicked
            val encoded = URLEncoder.encode(
                boardName,
                StandardCharsets.UTF_8.toString()
            )
            navController.navigate("board_detail/${boardId.toInt()}/$encoded?source=FROM_CREATE") {
                popUpTo("home") { inclusive = false }
                launchSingleTop = true
            }
        },
        showToast = ::showToast
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
