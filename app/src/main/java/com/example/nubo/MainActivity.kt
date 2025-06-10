package com.example.nubo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.nubo.model.card.ShortformItem
import com.example.nubo.ui.screen.add.AddScreen
import com.example.nubo.ui.screen.home.HomeScreen
import com.example.nubo.ui.screen.learn.LearnScreen
import com.example.nubo.ui.screen.myBoard.MyBoardScreen
import com.example.nubo.ui.screen.profile.ProfileScreen
import com.example.nubo.ui.component.BottomNavBar
import com.example.nubo.ui.screen.card.ShortformListScreen
import com.example.nubo.ui.screen.myBoard.BoardDetailScreen
import com.example.nubo.ui.theme.NuboAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent{
            NuboAppTheme{
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 상세 화면에서는 BottomNavBar 숨기기
    val showBottomBar = currentRoute in listOf("home", "myboard", "add", "learn", "profile")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    selectedIndex = getSelectedIndex(currentRoute),
                    onItemSelected = { index ->
                        val route = when (index) {
                            0 -> "home"
                            1 -> "myboard"
                            2 -> "add"
                            3 -> "learn"
                            4 -> "profile"
                            else -> "home"
                        }
                        navController.navigate(route) {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(onMoreClick = { navController.navigate("learn") })
            }
            composable("myboard") {
                MyBoardScreen(navController) // 👈 NavController 전달
            }
            composable("add") {
                AddScreen()
            }
            composable("learn") {
                LearnScreen()
            }
            composable("profile") {
                ProfileScreen()
            }

            // 나의 보드 상세 화면
            composable(
                "board_detail/{boardId}/{boardTitle}"
            ) { backStackEntry ->
                val boardId = backStackEntry.arguments?.getString("boardId") ?: ""
                val boardTitle = backStackEntry.arguments?.getString("boardTitle") ?: "로딩 중..."
                BoardDetailScreen(
                    boardId = boardId,
                    boardTitle = boardTitle,
                    navController = navController
                )
            }
        }
    }
}

fun getSelectedIndex(route: String?): Int {
    return when (route) {
        "home" -> 0
        "myboard" -> 1
        "add" -> 2
        "learn" -> 3
        "profile" -> 4
        else -> -1
    }
}
