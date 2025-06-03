package com.example.nubo.data.model

import com.example.nubo.R

sealed class BottomNavItem(
    val label: String,
    val iconRes: Int,
    val selectedIconRes: Int
) {
    object Home : BottomNavItem("홈", R.drawable.nav_home_unselected, R.drawable.nav_home_selected)
    object Board : BottomNavItem("보드",
        R.drawable.nav_dashboard_unselected,
        R.drawable.nav_dashboard_selected
    )
    object Add : BottomNavItem("추가", R.drawable.nav_add_unselected, R.drawable.nav_add_selected)
    object Learn : BottomNavItem("학습", R.drawable.nav_book_unselected, R.drawable.nav_book_selected)
    object Profile : BottomNavItem("마이",
        R.drawable.nav_profile_unselected,
        R.drawable.nav_profile_selected
    )

    companion object {
        val items = listOf(Home, Board, Add, Learn, Profile)
    }
}
