package com.example.nubo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.nubo.model.card.CardDetailDialogItem
import com.example.nubo.ui.screen.add.AddScreen
import com.example.nubo.ui.screen.home.HomeScreen
import com.example.nubo.ui.screen.learn.LearnScreen
import com.example.nubo.ui.screen.myBoard.MyBoardScreen
import com.example.nubo.ui.screen.profile.ProfileScreen
import com.example.nubo.ui.component.BottomNavBar
import com.example.nubo.ui.screen.card.ShortformListScreen
import com.example.nubo.ui.screen.myBoard.BoardDetailScreen
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
    val showBottomBar = currentRoute in listOf("home", "myboard", "add", "learn", "profile")

    // 콘텐츠 추가 전역 시트 상태
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var sheetVisible by remember { mutableStateOf(false) }


    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    selectedIndex = getSelectedIndex(currentRoute),
                    onItemSelected = { index ->
                        when (index) {
                            0 -> navController.navigate("home") { popUpTo("home"); launchSingleTop = true }
                            1 -> navController.navigate("myboard") { popUpTo("home"); launchSingleTop = true }
                            2 -> sheetVisible = true // + 버튼 누르면 시트 열기
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
                composable("profile") { ProfileScreen() }
                composable(
                    "board_detail/{boardId}/{boardTitle}"
                ) { backStackEntry ->
                    val boardId = backStackEntry.arguments?.getString("boardId") ?: ""
                    val boardTitle = backStackEntry.arguments?.getString("boardTitle") ?: "로딩 중..."
                    BoardDetailScreen(boardId = boardId, boardTitle = boardTitle, navController = navController)
                }
            }
        }
    }

    // 전역 모달 바텀 시트
    if (sheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { sheetVisible = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 15.dp, end = 15.dp, top = 0.dp, bottom = 15.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("추가 생성하기", style = AppTextStyles.b2_semibold_16)

                Spacer(Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    SheetOptionButton(
                        icon = Icons.Outlined.Image,
                        text = "영상",
                        onClick = {}
                    )
                    SheetOptionButton(
                        icon = Icons.Outlined.Folder,
                        text = "보드",
                        onClick = {  }
                    )
                }
            }
        }
    }
}

@Composable
fun SheetOptionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            color = Grey20,
            tonalElevation = 1.dp,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Grey700
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(text, style = AppTextStyles.b3_medium_14)
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
