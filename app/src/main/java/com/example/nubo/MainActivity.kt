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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.example.nubo.deeplink.DeepLinkContract
import com.example.nubo.deeplink.DeepLinkStore
import com.example.nubo.ui.component.BottomNavBar
import com.example.nubo.ui.component.sheet.BottomSheetHost
import com.example.nubo.ui.component.sheet.SheetRoute
import com.example.nubo.ui.screen.card.CardDetailRoute
import com.example.nubo.ui.screen.card.CardDetailViewModel
import com.example.nubo.ui.screen.editCard.EditCardRoute
import com.example.nubo.ui.screen.home.HomeScreen
import com.example.nubo.ui.screen.learn.LearnScreen
import com.example.nubo.ui.screen.myBoard.BoardDetailScreen
import com.example.nubo.ui.screen.myBoard.MyBoardScreen
import com.example.nubo.ui.screen.myBoard.SectionDetailScreen
import com.example.nubo.ui.screen.notification.NotificationScreen
import com.example.nubo.ui.screen.notification.NotificationViewModel
import com.example.nubo.ui.screen.onBoardingLogin.OnBoardingLoginActivity
import com.example.nubo.ui.screen.profile.EditNameScreen
import com.example.nubo.ui.screen.profile.InformationScreen
import com.example.nubo.ui.screen.profile.NotificationSetScreen
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

    // Pipe to deliver deep link intents into Compose world
    private val deepLinkEvents = MutableSharedFlow<Intent>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make system bars transparent and set icon appearance
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        // Cache initial intent + emit
        intent?.let { cacheDeepLinkIfAny(it); deepLinkEvents.tryEmit(it) }

        setContent {
            NuboAppTheme {
                RequestNotificationPermissionOnce() // Android 13+ POST_NOTIFICATIONS permission
                MainScreen(deepLinkEvents = deepLinkEvents)
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

// Cache deep link payload if presents in the intent
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
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isEditScreen = currentRoute?.startsWith("card_edit") == true
    val isCardDetail = currentRoute?.startsWith("card_detail") == true

    // Hide BottomNav on detail-like screens
    val showBottomBar = currentRoute in listOf(
        "home", "myboard", "add", "learn", "profile", "information", "notification"
    )

    var sheetRoute by remember { mutableStateOf<SheetRoute?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

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
                composable("home") {
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
                        // top-right notification button on Home
                        onNotificationsClick = {
                            navController.navigate("notification") {
                                launchSingleTop = true
                                popUpTo("home") { inclusive = false }
                            }
                        },
//                        modifier = Modifier.statusBarsPadding()
                    )
                }

                composable("myboard") {
                    MyBoardScreen(
                        navController = navController,
                        modifier = Modifier
                            .padding(innerPadding)
                            .statusBarsPadding()
                    )
                }

                composable("learn") {
                    // Learn keeps its own insets strategy
                    LearnScreen()
                }

                composable("profile") {
                    ProfileRoute(
                        navController = navController,
                        onBack = { navController.popBackStack() },
                        onMyInfo = { navController.navigate("information") },
                        modifier = Modifier.padding(innerPadding),
                        onNotification = { navController.navigate("notificationSet") }
                    )
                }

                composable(
                    route = "board_detail/{boardId}/{boardTitle}",
                    arguments = listOf(
                        navArgument("boardId") { type = NavType.IntType },
                        navArgument("boardTitle") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val boardId = backStackEntry.arguments?.getInt("boardId") ?: return@composable
                    val boardTitle = backStackEntry.arguments?.getString("boardTitle") ?: "로딩 중..."
                    BoardDetailScreen(
                        boardId = boardId,
                        boardTitle = boardTitle,
                        navController = navController,
                        modifier = Modifier.statusBarsPadding()
                    )
                }

                composable(
                    route = "section_detail/{sectionId}/{sectionTitle}",
                    arguments = listOf(
                        navArgument("sectionId") { type = NavType.IntType },
                        navArgument("sectionTitle") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val sectionId = backStackEntry.arguments?.getInt("sectionId") ?: return@composable
                    val sectionTitle = backStackEntry.arguments?.getString("sectionTitle") ?: "로딩 중..."
                    SectionDetailScreen(
                        sectionId = sectionId,
                        sectionTitle = sectionTitle,
                        navController = navController,
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
                        onAlarmSetting = { /* navigate to settings if added */ },
                        onClickItem = { item -> nvm.onClickItem(item) },
                        onAcceptInvite = { item -> nvm.onClickPrimary(item) },
                        onRejectInvite = { item -> nvm.onClickSecondary(item) },
                        onShowMore = { _ -> nvm.onClickMore() }
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
                    val refresh by refreshFlow.collectAsState(initial = false)

                    // Obtain VM scoped to this backStackEntry
                    val detailVm: CardDetailViewModel = hiltViewModel(backStackEntry)

                    // Initial load
                    LaunchedEffect(cardId) { detailVm.refresh() }

                    // Reload after edit
                    LaunchedEffect(refresh) {
                        if (refresh) {
                            detailVm.refresh()
                            backStackEntry.savedStateHandle["refresh_detail"] = false
                        }
                    }

                    Box(Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
//                        .statusBarsPadding()
                    ) {
                        CardDetailRoute(
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
            }
    }

    // Deep link handling — consume cached ones after login state resolved
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            startOnboardingForLogin(context)
            return@LaunchedEffect
        }

        // Card detail
        DeepLinkStore.pendingCardId?.let { id ->
            DeepLinkStore.pendingCardId = null
            navController.navigate("card_detail/${id.toInt()}") {
                popUpTo("home") { inclusive = false }
                launchSingleTop = true
            }
        }

        // Unread list → learn
        if (DeepLinkStore.pendingGoUnread) {
            DeepLinkStore.pendingGoUnread = false
            navController.navigate("learn") {
                popUpTo("home") { inclusive = false }
                launchSingleTop = true
            }
        }

        // Board detail / invite
        DeepLinkStore.pendingBoardId?.let { bId ->
            val title = DeepLinkStore.pendingBoardTitle ?: "로딩 중..."
            DeepLinkStore.pendingBoardId = null
            DeepLinkStore.pendingBoardTitle = null

            val encoded = URLEncoder.encode(title, StandardCharsets.UTF_8.toString())
            navController.navigate("board_detail/${bId.toInt()}/$encoded") {
                popUpTo("home") { inclusive = false }
                launchSingleTop = true
            }
            // pendingInviteToken is consumed in detail as needed
        }
    }

    // Runtime deep link events
    LaunchedEffect(Unit) {
        deepLinkEvents.collectLatest { intent ->
            val target = intent.getStringExtra(DeepLinkContract.EXTRA_DEEPLINK_TARGET)

            if (!isLoggedIn) {
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
            // TODO Create board via ViewModel
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
