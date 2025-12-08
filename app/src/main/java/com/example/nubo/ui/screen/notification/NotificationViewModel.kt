package com.example.nubo.ui.screen.notification

import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.repository.AuthRepository
import com.example.nubo.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class NotiEvent {
    data class GoCardDetail(val cardId: String) : NotiEvent()
    object GoLearn : NotiEvent()
    object GoNotificationCenter : NotiEvent()
    object GoHome : NotiEvent()
    data class GoBoard(val boardId: String) : NotiEvent()
}

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val authRepository: AuthRepository
): ViewModel(){

    // 화면 상태
    private val _uiState = MutableStateFlow(NotificationFeedState())
    val uiState: StateFlow<NotificationFeedState> = _uiState.asStateFlow()

    // 일회성 이벤트 채널
    private val _events = Channel<NotiEvent>(Channel.BUFFERED)
    val events: Flow<NotiEvent> = _events.receiveAsFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // API 26 이상에서만 안전하게 호출
                    repository.loadFeed()
                } else {
                    // 하위 버전용 대체 처리 (예: 빈 리스트 반환 또는 다른 계산 방식)
                    NotificationFeedState(emptyList(),emptyList(),loading = false)
                }
            }.onSuccess { feed ->
                    _uiState.value = feed.copy(loading = false)
            }.onFailure { _uiState.update { s -> s.copy(loading = false) } }
        }
    }

    fun onClickItem(item: NotificationItem) {
        // 먼저 낙관적 읽음 처리
        markReadLocal(item.notificationId)

        viewModelScope.launch {
            runCatching {
                repository.markRead(item.notificationId)
            }.onSuccess {
                // 서버 반영 후 전체 플래그 재조회
                val token = authRepository.getAccessToken()
                if (!token.isNullOrBlank()) {
                    repository.refreshUnreadFlag(token)
                }
            }
        }

        // 타입에 따라 라우팅 이벤트 발생
        when (item.type) {
            NotiType.UnviewedReminder -> {
                if (item.cardId != null) {
                    emit(NotiEvent.GoCardDetail(item.cardId))
                } else {
                    emit(NotiEvent.GoHome)
                }
            }
            NotiType.NewCard -> {
                item.cardId?.let { emit(NotiEvent.GoCardDetail(it)) }
            }
            NotiType.Invite -> {
                emit(NotiEvent.GoNotificationCenter)
            }
            NotiType.System -> {
                item.boardId?.let { emit(NotiEvent.GoBoard(it)) }
            }
        }
    }

    fun onClickPrimary(item: NotificationItem) {
        Log.d("NOTI", "accept click: nid=${item.notificationId}, iid=${item.invitationId}")
        if (item.invitationId == null) return

        setActionLoading(item.notificationId, true)

        viewModelScope.launch {
            val token = authRepository.getAccessToken()?: return@launch

            val invId: Int = when (val raw = item.invitationId) {
                is Int -> raw
                is String -> raw.toIntOrNull() ?: return@launch
                else -> return@launch
            }


            runCatching { repository.acceptInvitation(token, invId) }
            .onSuccess {
                Log.d("NOTI", "accept onSuccess")
                applyInviteResultLocally(
                    notificationId = item.notificationId,
                    accepted = true,
                    invitationId = item.invitationId
                )

//                새로 고침
                refresh()

                setActionLoading(item.notificationId, false)
            }.onFailure { e ->
                Log.e("NOTI", "accept onFailure: ${e.javaClass.simpleName}: ${e.message}", e)
                    setActionLoading(item.notificationId, false)

            }
        }
    }

    fun onClickSecondary(item: NotificationItem) {
        if (item.invitationId == null) return

        setActionLoading(item.notificationId, true)

        viewModelScope.launch {
            val token = authRepository.getAccessToken() ?: return@launch

            val invId: Int = when (val raw = item.invitationId) {
                is Int -> raw
                is String -> raw.toIntOrNull() ?: return@launch
                else -> return@launch
            }

            runCatching { repository.rejectInvitation(token, invId) }
            .onSuccess {
                applyInviteResultLocally(
                    notificationId = item.notificationId,
                    accepted = false,
                    invitationId = item.invitationId
                )


                // 서버 최신 목록 다시 조회
                refresh()

                setActionLoading(item.notificationId, false)
            }.onFailure {
                    setActionLoading(item.notificationId, false)
            }
        }
    }

    // 초대 수락/거절 성공 시 로컬 목록 반영
    // 알림을 목록에서 제거
    private fun applyInviteResultLocally(
        notificationId: String?,
        accepted: Boolean,
        invitationId: Any? = null
    ) {
        Log.d("NOTI", "applyInviteResultLocally accepted=$accepted nid=$notificationId iid=$invitationId")

        _uiState.update { s ->
            val wantInv = when (invitationId) {
                is Int -> invitationId.toString()
                is String -> invitationId
                else -> null
            }?.trim()

            fun List<NotificationItem>.removeTarget(): List<NotificationItem> = filterNot { it ->
                val byInvitation = !wantInv.isNullOrEmpty() &&
                    it.invitationId?.toString()?.trim() == wantInv

                val byNotificationId = !notificationId.isNullOrEmpty() &&
                    it.notificationId == notificationId

                byInvitation || byNotificationId
            }

            s.copy(
                recent = s.recent.removeTarget(),
                past   = s.past.removeTarget()
            )

        }
    }

    fun onClickMore() {
        // 스크린이 펼침 수를 관리하므로 여기서는 별도 동작 없음(추후 분석 이벤트 등 가능)
    }

    fun onClickMarkAllRead() {
        // 1) 체크해서 전부 이미 읽었으면 리턴
        val hasUnread = _uiState.value.recent.any { it.unread } ||
            _uiState.value.past.any { it.unread }
        if (!hasUnread) return

        // 2) UI 먼저 모두 읽음 처리 (optimistic update)
        _uiState.update { s ->
            s.copy(
                recent = s.recent.map { it.copy(unread = false) },
                past   = s.past.map   { it.copy(unread = false) }
            )
        }

        // 3) 서버에 일괄 읽음 요청
        viewModelScope.launch {
            runCatching {
                repository.markAllRead()
            }.onFailure { e ->
                // 필요하면 여기서 롤백 로직 추가 가능
                android.util.Log.e("NOTI", "markAllRead failed: ${e.message}", e)
            }
        }
    }


    private fun markReadLocal(notificationId: String) {
        _uiState.update { s ->
            s.copy(
                recent = s.recent.map { if (it.notificationId == notificationId) it.copy(unread = false) else it },
                past = s.past.map { if (it.notificationId == notificationId) it.copy(unread = false) else it }
            )
        }
    }

    private fun emit(e: NotiEvent) {
        viewModelScope.launch { _events.send(e) }
    }

    private fun setActionLoading(notificationId: String, loading: Boolean) {
        _uiState.update { s ->
            s.copy(
                actionLoadingIds = if (loading) s.actionLoadingIds + notificationId
                else s.actionLoadingIds - notificationId
            )
        }
    }
}
