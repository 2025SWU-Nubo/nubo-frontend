package com.example.nubo.ui.component.sheet


import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.components.toast.AppToastLayout
import com.example.components.toast.AppToastOverlay
import com.example.components.toast.AppToastType
import com.example.components.toast.rememberAppToastHostState
import com.example.nubo.domain.model.InviteUser
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
    onClickCreatedBoard: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
    showToast: (
        String,               // message
        AppToastType,         // type
        Int,                  // durationMillis
        Int,                  // preDelayMillis
        String?,              // actionLabel (nullable)
        (() -> Unit)?         // onAction (nullable)
    ) -> Unit
) {
    if (route == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 참여자 초대 상대 관리
//    var invitedEmails by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var inviteResetVersion by remember { mutableStateOf(0) }

    // 참여자 프리뷰 정보 (닉네임, 프로필 이미지)
    var invitedUserPreview by remember { mutableStateOf<List<InviteUser>>(emptyList()) }

    val createBoardViewModel: CreateBoardViewModel = hiltViewModel()
    val ui by createBoardViewModel.ui.collectAsState() // CreateBoardUiState(name, isShared, isLoading, nameError, created)

    var pendingToastMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // 보드 생성 완료 시 토스트 + 콜백 처리
    LaunchedEffect(ui.created) {
        ui.created?.let { created ->

            // 1 초대 상태 먼저 리셋
            inviteResetVersion++          // InviteViewModel 에 "초기화 신호" 보내기
            invitedUserPreview = emptyList()
            createBoardViewModel.setInvitedEmails(emptyList())
            createBoardViewModel.resetForNewBoard()   // 필요하다면 전체 초기화

            // 2 토스트 + 상위 콜백
            showToast(
                "보드 생성이 완료되었어요.",
                AppToastType.NORMAL,
                2600,
                550,
                "바로가기",                           // actionLabel
                { onClickCreatedBoard(created.id, created.name) }  // onAction: 네비게이션 콜백
            )

            // 기존 상위 처리 유지 (리스트 갱신 등)
            onCreateBoard(created.name, ui.isShared)
            // 3 created 플래그 소비
            createBoardViewModel.consumeCreated()
            // 4 부모에 시트 닫기 알리기
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            // 1) invited 상태 초기화
            inviteResetVersion++
            createBoardViewModel.setInvitedEmails(emptyList())
            invitedUserPreview = emptyList()

            // 2) board 생성 UI 전체 리셋
            createBoardViewModel.resetForNewBoard()

            // 3) 외부에 시트 닫힘 알리기 (route = null 등)
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = Color.White,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier
    ) {
        // Invite 시트일 때만 시스템 뒤로가기를 가로채서
        // 바텀시트 dismiss 대신 "이전 시트로 이동" 처리
        BackHandler(enabled = true) {
            when (route) {
                SheetRoute.Invite -> {
                    // 참여자 초대 시트 -> 보드 만들기 시트로만 이동
                    onBackToCreateBoard()
                }

                SheetRoute.CreateBoard -> {
                    // 보드 만들기 시트 -> 추가 생성하기 시트로 이동
                    // (공유 보드였다면 선택한 참여자도 초기화)
                    if (ui.isShared) {
                        inviteResetVersion++
                        createBoardViewModel.setInvitedEmails(emptyList())
                        invitedUserPreview = emptyList()
                    }
                    onBackToAddMenu()
                }

                SheetRoute.AddMenu -> {
                    // 추가 생성하기 시트에서 뒤로가기 -> 바텀시트 닫기
                    inviteResetVersion++
                    createBoardViewModel.setInvitedEmails(emptyList())
                    invitedUserPreview = emptyList()
                    onDismiss()
                }

                SheetRoute.AddVideo -> {
                    // 영상 추가 시트가 있다면:
                    // 원하면 AddMenu로만 돌아가도 되고, 바로 닫아도 됨
                    onBackToAddMenu()
                }

                null -> {
                    // 안전장치: route가 null이면 그냥 닫기
                    inviteResetVersion++
                    createBoardViewModel.setInvitedEmails(emptyList())
                    invitedUserPreview = emptyList()
                    onDismiss()
                }
            }
        }

        LaunchedEffect(route, pendingToastMessage) {
            if (route == SheetRoute.CreateBoard && pendingToastMessage != null) {
                showToast(
                    pendingToastMessage!!, // 예약된 메시지 사용
                    AppToastType.POSITIVE,
                    1600,
                    160, // 160ms 지연 후 CreateBoardSheet 위에 토스트가 뜸
                    null,
                    null
                )
                pendingToastMessage = null // 토스트를 띄웠으므로 상태를 비움
            }
        }
        AnimatedContent(
            targetState = route,
            transitionSpec = {
                if (isForwardFrom(initialState, targetState)) {
                    // forward: 아래에서 위로 등장, 기존 것은 위로 사라짐
                    slideInVertically(
                        animationSpec = tween(200)
                    ) { fullHeight -> fullHeight } togetherWith
                        slideOutVertically(
                            animationSpec = tween(200)
                        ) { fullHeight -> -fullHeight }
                } else {
                    // backward: 위에서 아래로 등장, 기존 것은 아래로 사라짐
                    slideInVertically(
                        animationSpec = tween(280)
                    ) { fullHeight -> -fullHeight } togetherWith
                        slideOutVertically(
                            animationSpec = tween(280)
                        ) { fullHeight -> fullHeight }
                }.using(
                    SizeTransform(clip = false) // avoid clipping during animation
                )
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
//                            ui.invitedEmails = emptyList()
                            inviteResetVersion++
                            createBoardViewModel.setInvitedEmails(emptyList())
                        }
                        onBackToAddMenu()
                    },
                    onInviteClick = onGoInvite,
                    onCreate = {_,_ ->},
                    name = ui.name,
                    isShared = ui.isShared,
                    invitedEmails = ui.invitedEmails,
                    invitedUsers = invitedUserPreview,
                    onNameChange = createBoardViewModel::onNameChange,
                    onSharedChange = { shared ->
                        if (ui.isShared && !shared) {
//                            invitedEmails = emptyList()
                            inviteResetVersion++
                            createBoardViewModel.setInvitedEmails(emptyList())
                        }
                        createBoardViewModel.onSharedChange(shared)
                    },
                    isLoading = ui.isLoading,
                    nameError = ui.nameError,
                    onSubmit = { nameText ->           // ← 시그니처 바뀜 (아래 3번 참고)
                        createBoardViewModel.submitWith(nameText)
                    } ,
                )
                SheetRoute.Invite -> InviteSheet(
                    onClose = onDismiss,
                    onBack = onBackToCreateBoard,
                    onInvite = onInvite,
                    resetSignal = inviteResetVersion,
                    initialSelected = ui.invitedEmails,
                    onComplete = { emails, users ->
//                        invitedEmails = emails
                        invitedUserPreview = users
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
                    showToast = { msg, type, duration ->
                        // 바텀시트 안에서 호출한 토스트를
                        // MainScreen 에서 만든 전역 토스트로 전달
                        showToast(msg, type, duration, 900, null, null )   // preDelay = 0
                    }
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


