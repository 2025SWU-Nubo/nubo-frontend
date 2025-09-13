package com.example.nubo.ui.component.sheet


import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel


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

    // 참여자 초대 상대 관리
    var invitedEmails by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var inviteResetVersion by remember { mutableStateOf(0) }

    val createBoardViewModel: CreateBoardViewModel = hiltViewModel()
    val ui by createBoardViewModel.ui.collectAsState() // CreateBoardUiState(name, isShared, isLoading, nameError, created)

    val context = LocalContext.current

    LaunchedEffect(ui.created) {
        ui.created?.let { created ->
            val typeKo = if (ui.isShared) "공유" else "개인"
            Toast.makeText(
                context,
                "‘${created.name}’ ${if (ui.isShared) "공유" else "개인"} 보드를 생성했어요.",
                Toast.LENGTH_SHORT
            ).show()
            // 1) 상위에 알림 (여기서는 상위가 리스트 새로고침 등을 하도록 name/shared 전달)
            onCreateBoard(ui.name.trim(), ui.isShared)
            // 2) VM 내부 created 신호 소비 및 입력 초기화
            createBoardViewModel.consumeCreated()
            // 3) 시트 닫기
            onDismiss()
        }
    }


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
                    onBack = {
                        if (ui.isShared) {
                            invitedEmails = emptyList()
                            inviteResetVersion++
                            createBoardViewModel.setInvitedEmails(emptyList())
                        }
                        onBackToAddMenu()
                    },
                    onInviteClick = onGoInvite,
                    onCreate = {_,_ ->},
                    name = ui.name,
                    isShared = ui.isShared,
                    onNameChange = createBoardViewModel::onNameChange,
                    onSharedChange = { shared ->
                        if (ui.isShared && !shared) {
                            invitedEmails = emptyList()
                            inviteResetVersion++
                            createBoardViewModel.setInvitedEmails(emptyList())
                        }
                        createBoardViewModel.onSharedChange(shared)
                    },
                    isLoading = ui.isLoading,
                    nameError = ui.nameError,
                    onSubmit = createBoardViewModel::submit
                )
                SheetRoute.Invite -> InviteSheet(
                    onClose = onDismiss,
                    onBack = onBackToCreateBoard,
                    onInvite = onInvite,
                    resetSignal = inviteResetVersion,
                    onComplete = { emails ->
                        invitedEmails = emails
                        // 1) 부모에 초대 이메일 전달(서버 전송은 부모/VM에서 처리 권장)
                        createBoardViewModel.setInvitedEmails(emails)
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


