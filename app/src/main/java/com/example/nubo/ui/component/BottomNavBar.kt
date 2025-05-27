package com.example.nubo.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.example.nubo.R

@Composable
fun BottomNavBar(selectedIndex: Int = 0, onItemSelected: (Int) -> Unit = {}) {
    NavigationBar(
        containerColor = Color.White
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
            label = { Text("홈") },
            selected = selectedIndex == 0,
            onClick = { onItemSelected(0) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = Color.Gray,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedTextColor = Color.Gray,
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
            label = { Text("보드") },
            selected = selectedIndex == 1,
            onClick = { onItemSelected(1) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = Color.Gray,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedTextColor = Color.Gray,
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
            label = { Text("추가") },
            selected = selectedIndex == 2,
            onClick = { onItemSelected(2) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = Color.Gray,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )

        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(
                        id = if (selectedIndex == 3) R.drawable.nav_book_selected else R.drawable.nav_book_unselected
                    ),
                    contentDescription = "학습"
                )
            },
            label = { Text("학습") },
            selected = selectedIndex == 3,
            onClick = { onItemSelected(3) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = Color.Gray,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedTextColor = Color.Gray,
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
            label = { Text("마이") },
            selected = selectedIndex == 4,
            onClick = { onItemSelected(4) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = Color.Gray,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
    }
}


//@Composable
//fun BottomNavBar(selectedIndex: Int =0, onItemSelected: (Int)-> Unit = {}) {
//    NavigationBar(
//        containerColor = Color.White
//    ) {
//        NavigationBarItem(icon = { Icon(
//            painter = painterResource(
//                id = if (selectedIndex == 0) R.drawable.nav_home_selected else R.drawable.nav_home_unselected
//            ),
//            contentDescription = "홈",
//            tint = MaterialTheme.colorScheme.primary
//        ) },
//            selected = selectedIndex == 0,
//            onClick = {onItemSelected(0)})
//
//        NavigationBarItem(icon = { Icon(
//            painter = painterResource(
//                id = if (selectedIndex == 1) R.drawable.nav_dashboard_selected else R.drawable.nav_dashboard_unselected
//            ),
//            contentDescription = "나의 보드",
//            tint = MaterialTheme.colorScheme.primary
//        ) },
//            selected = selectedIndex == 1,
//            onClick = {onItemSelected(1)})
//
//        NavigationBarItem(icon = { Icon(
//            painter = painterResource(
//                id = if (selectedIndex == 2) R.drawable.nav_add_selected else R.drawable.nav_add_unselected
//            ),
//            contentDescription = "컨텐츠 추가",
//            tint = MaterialTheme.colorScheme.primary
//        ) },
//            selected = selectedIndex == 2,
//            onClick = {onItemSelected(2)})
//
//        NavigationBarItem(icon = { Icon(
//            painter = painterResource(
//                id = if (selectedIndex == 3) R.drawable.nav_book_selected else R.drawable.nav_book_unselected
//            ),
//            contentDescription = "학습 공간",
//            tint = MaterialTheme.colorScheme.primary
//        ) },
//            selected = selectedIndex == 3,
//            onClick = {onItemSelected(3)})
//
//        NavigationBarItem(icon = { Icon(
//            painter = painterResource(
//                id = if (selectedIndex == 4) R.drawable.nav_profile_selected else R.drawable.nav_profile_unselected
//            ),
//            contentDescription = "마이페이지",
//            tint = MaterialTheme.colorScheme.primary
//        ) },
//            selected = selectedIndex == 4,
//            onClick = {onItemSelected(4)})
//    }
//}
