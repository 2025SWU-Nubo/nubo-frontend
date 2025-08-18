package com.example.nubo.ui.component.sheet

// Bottom-sheet routing states
sealed interface SheetRoute {
    data object AddMenu : SheetRoute
    data object CreateBoard : SheetRoute
    data object Invite : SheetRoute
}
