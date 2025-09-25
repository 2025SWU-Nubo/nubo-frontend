package com.example.nubo

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.nubo.ui.screen.home.HomeScreen
import com.example.nubo.ui.screen.learn.LearnScreen
import com.example.nubo.ui.screen.myBoard.MyBoardScreen
import com.example.nubo.ui.component.BottomNavBar
import com.example.nubo.ui.component.sheet.BottomSheetHost
import com.example.nubo.ui.component.sheet.SheetRoute
import com.example.nubo.ui.screen.card.CardDetailRoute
import com.example.nubo.ui.screen.card.CardDetailViewModel
import com.example.nubo.ui.screen.card.EditCardRoute
import com.example.nubo.ui.screen.myBoard.BoardDetailScreen
import com.example.nubo.ui.screen.profile.EditNameScreen
import com.example.nubo.ui.screen.profile.InformationScreen
import com.example.nubo.ui.screen.profile.ProfileRoute
import com.example.nubo.ui.theme.NuboAppTheme
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 시스템바를 투명하게 설정 (핵심 부분)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        // 상태바/내비게이션바 아이콘 색 (밝은 배경일 경우 true = 어두운 아이콘)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true

        setContent {
            NuboAppTheme {
                MainScreen()
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 상세 화면에서는 BottomNavBar 숨기기
    val showBottomBar = currentRoute in listOf("home", "myboard", "add", "learn", "profile", "information")
    var sheetRoute by remember { mutableStateOf<SheetRoute?>(null) }

    val contentInsets =
        WindowInsets.safeDrawing

    Scaffold(

        // 프로필 화면일 때만 시스템 인셋 자동패딩 제거
        contentWindowInsets = if (currentRoute == "profile" || currentRoute == "information" || currentRoute == "edit_name?initial={initial}") {
            WindowInsets(0)
        } else {
            contentInsets
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
                    }

                )
            }
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
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
                        onOpenCardDetail = {id -> navController.navigate("card_detail/$id")}
                    )
                }
                composable("myboard") { MyBoardScreen(navController) }
                composable("learn") { LearnScreen() }
                composable("profile") {
                    // ViewModel과 묶인 Route로 교체
                    ProfileRoute(
                        navController = navController,
                        onBack = { navController.popBackStack() },
                        onMyInfo = { navController.navigate("information") }
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
                        navController = navController
                    )
                }
//                composable(
//                    "board_detail/{boardId}/{boardTitle}"
//                ) { backStackEntry ->
//                    val boardId = backStackEntry.arguments?.getInt("boardId") ?: return@composable
//                    val boardTitle = backStackEntry.arguments?.getString("boardTitle") ?: "로딩 중..."
//                    BoardDetailScreen(boardId = boardId, boardTitle = boardTitle, navController = navController)
//                }
                composable("information") {
                    InformationScreen(
                        navController = navController,
                        onBack = { navController.popBackStack() }, // 뒤로가기
                        onEditProfileImage = { /* 편집 처리 */ },
                        onLogout = { /* 로그아웃 처리 */ },
                        onWithdraw = { /* 탈퇴 처리 */ },
                        onEditName = { current -> navController.navigate("edit_name?initial=${Uri.encode(current)}") }
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
                                ?.remove<String>("edited_name")   // ← 취소 시 잔여값 제거
                            navController.popBackStack()
                        },
                        onDone = { newName ->
                            // 값 반환 후 이전 화면으로
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("edited_name", newName)
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    route = "card_detail/{cardId}",
                    arguments = listOf(navArgument("cardId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val cardId = backStackEntry.arguments?.getInt("cardId") ?: return@composable

                    // 노트 수정 페이지에서 돌아올 경으 true로 세팅, 서버 재요청
                    val refreshFlow = remember(backStackEntry) {
                        backStackEntry.savedStateHandle.getStateFlow("refresh_detail",false)
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
