package com.example.nubo.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey500
import com.example.nubo.ui.theme.Grey900

@Composable
fun BottomNavBar(selectedIndex: Int = 0, onItemSelected: (Int) -> Unit = {},isLearnScreen: Boolean = false,modifier: Modifier = Modifier) {
    NavigationBar(
        // learn 화면만 반투명
        containerColor = if (isLearnScreen) Color.White.copy(alpha = 0.7f) else Color.White,

    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(
                        id = if (selectedIndex == 0) R.drawable.nav_home_selected else R.drawable.nav_home_unselected
                    ),
                    contentDescription = "홈"
                )
            },
            label = { Text("홈", style =  if(selectedIndex==0)AppTextStyles.label_semibold_14 else AppTextStyles.label_medium_14) },
            selected = selectedIndex == 0,
            onClick = { onItemSelected(0) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = Grey900,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedTextColor = Grey500,
                indicatorColor = Color.Transparent
            )
        )

        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(
                        id = if (selectedIndex == 1) R.drawable.nav_dashboard_selected else R.drawable.nav_dashboard_unselected
                    ),
                    contentDescription = "나의 보드"
                )
            },
            label = { Text("보드", style = if(selectedIndex==1)AppTextStyles.label_semibold_14 else AppTextStyles.label_medium_14) },
            selected = selectedIndex == 1,
            onClick = { onItemSelected(1) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = Grey900,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedTextColor = Grey500,
                indicatorColor = Color.Transparent
            )
        )

        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(
                        id = if (selectedIndex == 2) R.drawable.nav_add_selected else R.drawable.nav_add_unselected
                    ),
                    contentDescription = "추가"
                )
            },
            label = { Text("추가", style = if(selectedIndex==2)AppTextStyles.label_semibold_14 else AppTextStyles.label_medium_14) },
            selected = selectedIndex == 2,
            onClick = { onItemSelected(2) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = Grey900,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedTextColor = Grey500,
                indicatorColor = Color.Transparent
            )
        )

        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(
                        id = if (selectedIndex == 3) R.drawable.nav_learn_selected else R.drawable.nav_learn_unselected
                    ),
                    contentDescription = "대시보드"
                )
            },
            label = { Text("대시보드", style = if(selectedIndex==3)AppTextStyles.label_semibold_14 else AppTextStyles.label_medium_14) },
            selected = selectedIndex == 3,
            onClick = { onItemSelected(3) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = Grey900,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedTextColor = Grey500,
                indicatorColor = Color.Transparent
            )
        )

        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(
                        id = if (selectedIndex == 4) R.drawable.nav_profile_selected else R.drawable.nav_profile_unselected
                    ),
                    contentDescription = "마이페이지"
                )
            },
            label = { Text("마이", style = if(selectedIndex==4)AppTextStyles.label_semibold_14 else AppTextStyles.label_medium_14) },
            selected = selectedIndex == 4,
            onClick = { onItemSelected(4) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = Grey900,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedTextColor = Grey500,
                indicatorColor = Color.Transparent
            )
        )
    }
}

