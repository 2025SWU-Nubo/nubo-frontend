package com.example.nubo.ui.screen.notification

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.model.AppNotification
import com.example.nubo.data.model.AppNotificationType
import com.example.nubo.data.model.NotificationDto
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
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

sealed class NotiEvent {
    data class GoCardDetail(val cardId: String) : NotiEvent()
    object GoLearn : NotiEvent()
    object GoNotificationCenter : NotiEvent()
    data class GoBoard(val boardId: String) : NotiEvent()
    data class InviteAccepted(val invitationId: String, val boardId: String?) : NotiEvent()
    data class InviteRejected(val invitationId: String, val boardId: String?) : NotiEvent()
}

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: NotificationRepository
): ViewModel(){

    // 화면에 보여줄 섹션 상태를 담는 데이터 클래스임
    data class UiState(
        val recent: List<AppNotification> = emptyList(), // 0~2일 섹션
        val past: List<AppNotification> = emptyList(),   // 3~7일 섹션
        val loading: Boolean = false,                    // 로딩 상태
        val error: String? = null
    )

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
            runCatching { repository.loadFeed() }
                .onSuccess { _uiState.value = it }
                .onFailure { _uiState.update { s -> s.copy(loading = false) } }
        }
    }

    fun onClickItem(item: NotificationItem) {
        // 먼저 낙관적 읽음 처리
        markReadLocal(item.notificationId)

        viewModelScope.launch {
            runCatching { repository.markRead(item.notificationId) }
        }

        // 타입에 따라 라우팅 이벤트 발생
        when (item.type) {
            NotiType.UnviewedReminder -> {
                if (item.cardId != null) {
                    emit(NotiEvent.GoCardDetail(item.cardId))
                } else {
                    emit(NotiEvent.GoLearn)
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
        // 초대 수락 버튼
        if (item.invitationId != null) {
            emit(NotiEvent.InviteAccepted(item.invitationId, item.boardId))
            // 실제 API가 있다면 여기서 호출하고 결과에 따라 목록 새로고침
        }
    }

    fun onClickSecondary(item: NotificationItem) {
        // 초대 거절 버튼
        if (item.invitationId != null) {
            emit(NotiEvent.InviteRejected(item.invitationId, item.boardId))
            // 실제 API가 있다면 여기서 호출하고 결과에 따라 목록 새로고침
        }
    }

    fun onClickMore() {
        // 스크린이 펼침 수를 관리하므로 여기서는 별도 동작 없음(추후 분석 이벤트 등 가능)
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
}
