package com.example.nubo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import com.example.nubo.ui.screen.add.AddScreen
import com.example.nubo.ui.screen.home.HomeScreen
import com.example.nubo.ui.screen.learn.LearnScreen
import com.example.nubo.ui.screen.myBoard.MyBoardScreen
import com.example.nubo.ui.screen.profile.ProfileScreen
import com.example.nubo.ui.component.BottomNavBar
import com.example.nubo.ui.theme.NuboAppTheme


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
fun MainScreen(){
    val selectedIndex = remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            BottomNavBar(
                selectedIndex = selectedIndex.intValue,
                onItemSelected = { selectedIndex.intValue = it }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedIndex.intValue) {
                0 -> HomeScreen(onMoreClick = { selectedIndex.intValue = 3})
                1 -> MyBoardScreen()
                2 -> AddScreen()
                3 -> LearnScreen()
                4 -> ProfileScreen()
            }
        }
    }
}







