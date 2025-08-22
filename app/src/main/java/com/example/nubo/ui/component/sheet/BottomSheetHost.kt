package com.example.nubo.ui.component.sheet


import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally



@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetHost(
    route: SheetRoute?,
    onDismiss: () -> Unit,
    onGoCreateBoard: () -> Unit,
    onGoInvite: () -> Unit,
    onCreateBoard: (String, Boolean) -> Unit,
    onInvite: (String) -> Unit,
    onGoAddVideo: () -> Unit,
    onBackToAddMenu: () -> Unit,
    onBackToCreateBoard: ()-> Unit,
    modifier: Modifier = Modifier
) {
    if (route == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier
    ) {
        AnimatedContent(
            targetState = route,
            transitionSpec = {
                if (isForwardFrom(initialState, targetState)) {
                    // 앞으로 이동
                    slideInHorizontally(tween(300)) { fullWidth -> fullWidth } togetherWith
                        slideOutHorizontally(tween(300)) { fullWidth -> -fullWidth }
                } else {
                    // 뒤로 이동
                    slideInHorizontally(tween(300)) { fullWidth -> -fullWidth } togetherWith
                        slideOutHorizontally(tween(300)) { fullWidth -> fullWidth }
                }
            },
            label = "BottomSheetSwitch"
        ) { target ->
            when (target) {
                SheetRoute.AddMenu -> AddMenuSheet(
                    onClose = onDismiss,
                    onVideoClick = onGoAddVideo,
                    onBoardClick = onGoCreateBoard
                )
                SheetRoute.CreateBoard -> CreateBoardSheet(
                    onClose = onDismiss,
                    onBack = onBackToAddMenu,
                    onInviteClick = onGoInvite,
                    onCreate = onCreateBoard
                )
                SheetRoute.Invite -> InviteSheet(
                    onClose = onDismiss,
                    onBack = onBackToCreateBoard,
                    onInvite = onInvite,

                )
                SheetRoute.AddVideo -> AddVideoSheet(
                    onClose = onDismiss
                )
            }
        }
    }
}

// 간단한 방향 판별 헬퍼
fun isForwardFrom(from: SheetRoute, to: SheetRoute): Boolean {
    val order = listOf(
        SheetRoute.AddMenu,
        SheetRoute.CreateBoard,
        SheetRoute.Invite,
        SheetRoute.AddVideo
    )
    return order.indexOf(to) > order.indexOf(from)
}


///
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun BottomSheetHost(
//    route: SheetRoute?,                     // which sheet to show
//    onDismiss: () -> Unit,                  // dismiss handler
//    onGoCreateBoard: () -> Unit,            // AddMenu -> CreateBoard
//    onGoInvite: () -> Unit,                 // CreateBoard -> Invite
//    onCreateBoard: (String, Boolean) -> Unit,// create board action
//    onInvite: (String) -> Unit,             // invite action
//    onGoAddVideo: () -> Unit,  // add video action
//    onBackToAddMenu: () -> Unit,
//    onBackToCreateBoard: ()-> Unit,
//    modifier: Modifier = Modifier
//) {
//    if (route == null) return
//
//    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
//
//    Crossfade(targetState = route, label = "BottomSheetSwitch") { target ->
//        when (target) {
//            SheetRoute.AddMenu -> AddMenuSheet(
//                onClose = onDismiss,
//                onVideoClick = onGoAddVideo,
//                onBoardClick = onGoCreateBoard
//            )
//            SheetRoute.CreateBoard -> CreateBoardSheet(
//                onClose = onDismiss,
//                onBack = onBackToAddMenu,
//                onInviteClick = onGoInvite,
//                onCreate = onCreateBoard
//            )
//            SheetRoute.Invite -> InviteSheet(
//                onClose = onDismiss,
//                onBack = onBackToCreateBoard,
//                onInvite = onInvite
//            )
//            SheetRoute.AddVideo -> AddVideoSheet(
//                onClose = onDismiss
//            )
//        }
//    }
//
//    ModalBottomSheet(
//        onDismissRequest = onDismiss,
//        sheetState = sheetState,
//        containerColor = Color.White,
//        contentColor = MaterialTheme.colorScheme.onSurface,
//        dragHandle = { BottomSheetDefaults.DragHandle() },
//        modifier = modifier
//    ) {
//        when (route) {
//            SheetRoute.AddMenu -> AddMenuSheet(
//                onClose = onDismiss,
//                onVideoClick = onGoAddVideo,
//                onBoardClick = onGoCreateBoard
//            )
//            SheetRoute.CreateBoard -> CreateBoardSheet(
//                onClose = onDismiss,
//                onBack = onBackToAddMenu,
//                onInviteClick = onGoInvite,
//                onCreate = onCreateBoard
//            )
//            SheetRoute.Invite -> InviteSheet(
//                onClose = onDismiss,
//                onBack = onBackToCreateBoard,
//                onInvite = onInvite
//            )
//            SheetRoute.AddVideo -> AddVideoSheet(
//                onClose = onDismiss
//            )
//        }
//    }
//}
