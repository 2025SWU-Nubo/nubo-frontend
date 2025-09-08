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
import androidx.compose.runtime.saveable.rememberSaveable


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
    onInviteComplete: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    if (route == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var createBoardName by rememberSaveable { mutableStateOf("") }
    var isShared by rememberSaveable { mutableStateOf(false) }

    // 참여자 초대 상대 관리
    var invitedEmails by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var inviteResetVersion by remember { mutableStateOf(0) }

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
                    onCreate = {
                               name,shared -> onCreateBoard(name,shared)
                               },
                    name = createBoardName,
                    isShared = isShared,
                    onNameChange = { createBoardName = it },
                    onSharedChange = { newShared ->
                        if (isShared && !newShared) {
                            invitedEmails = emptyList()
                            inviteResetVersion++   // InviteSheet 내부 선택도 리셋하게 신호
                        }
                        isShared = newShared
                    }
                )
                SheetRoute.Invite -> InviteSheet(
                    onClose = onDismiss,
                    onBack = onBackToCreateBoard,
                    onInvite = onInvite,
                    resetSignal = inviteResetVersion,
                    onComplete = { emails ->
                        invitedEmails = emails
                        // 1) 부모에 초대 이메일 전달(서버 전송은 부모/VM에서 처리 권장)
                        onInviteComplete(emails)
                        // 2) CreateBoard 시트로 되돌아가기
                        onBackToCreateBoard()
                    }
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


