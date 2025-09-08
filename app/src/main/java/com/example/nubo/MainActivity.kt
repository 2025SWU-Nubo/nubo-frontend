package com.example.nubo

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.nubo.model.card.CardDetailItem
import androidx.navigation.navArgument
import com.example.nubo.ui.screen.add.AddScreen
import com.example.nubo.ui.screen.home.HomeScreen
import com.example.nubo.ui.screen.learn.LearnScreen
import com.example.nubo.ui.screen.myBoard.MyBoardScreen
import com.example.nubo.ui.screen.profile.ProfileScreen
import com.example.nubo.ui.component.BottomNavBar
import com.example.nubo.ui.component.sheet.BottomSheetHost
import com.example.nubo.ui.component.sheet.SheetRoute
import com.example.nubo.ui.screen.card.ShortformListScreen
import com.example.nubo.ui.screen.myBoard.BoardDetailScreen
import com.example.nubo.ui.screen.profile.EditNameScreen
import com.example.nubo.ui.screen.profile.InformationScreen
import com.example.nubo.ui.theme.AppFonts
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey20
import com.example.nubo.ui.theme.Grey200
import com.example.nubo.ui.theme.Grey700
import com.example.nubo.ui.theme.NuboAppTheme
import dagger.hilt.android.AndroidEntryPoint

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

        setContent{
            NuboAppTheme{
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
    val showBottomBar = currentRoute in listOf("home", "myboard", "add", "learn", "profile","information")
    var sheetRoute by remember { mutableStateOf<SheetRoute?>(null) }


    Scaffold(

        // 프로필 화면일 때만 시스템 인셋 자동패딩 제거
        contentWindowInsets = if (currentRoute == "profile"||currentRoute == "information"||currentRoute == "edit_name?initial={initial}") {
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
                composable("home") { HomeScreen(onMoreClick = { navController.navigate("learn") }) }
                composable("myboard") { MyBoardScreen(navController) }
                composable("learn") { LearnScreen() }
                composable("profile") { ProfileScreen(onBack = { navController.popBackStack() },
                onMyInfo = { navController.navigate("information") }) }
                composable(
                    "board_detail/{boardId}/{boardTitle}"
                ) { backStackEntry ->
                    val boardId = backStackEntry.arguments?.getString("boardId") ?: ""
                    val boardTitle = backStackEntry.arguments?.getString("boardTitle") ?: "로딩 중..."
                    BoardDetailScreen(boardId = boardId, boardTitle = boardTitle, navController = navController)
                }
                composable("information") {
                    InformationScreen(
                        navController = navController,
                        onBack = { navController.popBackStack() },  // 뒤로가기
                        onEditProfileImage = { /* 편집 처리 */ },
                        onLogout = { /* 로그아웃 처리 */ },
                        onWithdraw = { /* 탈퇴 처리 */ },
                        onEditName = { current -> navController.navigate("edit_name?initial=${Uri.encode(current)}")}
                    )
                }
                composable(
                    route = "edit_name?initial={initial}",
                    arguments = listOf(navArgument("initial") { defaultValue = "" })
                ) { backStackEntry ->
                    val initial = backStackEntry.arguments?.getString("initial").orEmpty()

                    EditNameScreen(
                        initial = initial,
                        onBack = { navController.popBackStack() },
                        onDone = { newName ->
                            // 값 반환 후 이전 화면으로
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("edited_name", newName)
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
        onBackToCreateBoard = { sheetRoute = SheetRoute.CreateBoard},
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
