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
import androidx.compose.ui.text.AnnotatedString
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.components.toast.AppToastLayout
import com.example.components.toast.AppToastOverlay
import com.example.components.toast.AppToastType
import com.example.components.toast.rememberAppToastHostState
import kotlinx.coroutines.launch


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
    modifier: Modifier = Modifier,
    showToast: (String, AppToastType, Int,Int) -> Unit
) {
    if (route == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 참여자 초대 상대 관리
    var invitedEmails by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var inviteResetVersion by remember { mutableStateOf(0) }

    val createBoardViewModel: CreateBoardViewModel = hiltViewModel()
    val ui by createBoardViewModel.ui.collectAsState() // CreateBoardUiState(name, isShared, isLoading, nameError, created)

    var pendingToastMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // 성공 처리 (토스트 이후 상위 알림도 생성 결과 이름 사용 권장)
    LaunchedEffect(ui.created) {
        ui.created?.let { created ->
            showToast(
                "‘${created.name}’ ${if (ui.isShared) "공유" else "개인"} 보드를 생성했어요.",
                AppToastType.POSITIVE,
                1800,
                550
            )
            onCreateBoard(created.name, ui.isShared)
            createBoardViewModel.consumeCreated()
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

        LaunchedEffect(route, pendingToastMessage) {
            if (route == SheetRoute.CreateBoard && pendingToastMessage != null) {
                showToast(
                    pendingToastMessage!!, // 예약된 메시지 사용
                    AppToastType.POSITIVE,
                    1600,
                    160 // 160ms 지연 후 CreateBoardSheet 위에 토스트가 뜸
                )
                pendingToastMessage = null // 토스트를 띄웠으므로 상태를 비움
            }
        }
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
                    onSubmit = { nameText ->           // ← 시그니처 바뀜 (아래 3번 참고)
                        createBoardViewModel.submitWith(nameText)
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
                        createBoardViewModel.setInvitedEmails(emails)
                        onInviteComplete(emails)

                        // 2) 시트 전환 직후 자연스럽게 띄우기
                        val count = emails.size
                        if (count > 0) {
                            pendingToastMessage = "참여자 ${count}명 초대 완료!"
                        }

                        // 3) CreateBoard 시트로 되돌아가기
                        onBackToCreateBoard()

                    }
                    )
                SheetRoute.AddVideo -> AddVideoSheet(
                    onClose = onDismiss,
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


