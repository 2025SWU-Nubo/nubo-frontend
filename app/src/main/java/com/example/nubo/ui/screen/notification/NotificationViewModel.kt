package com.example.nubo.ui.screen.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class NotiType { UnviewedReminder, NewCard, Invite, System }

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timeLabel: String,
    val type: NotiType,
    val unread: Boolean = true,
    val action: NotiAction? = null,
)

/* 알림 버튼 타입 */
sealed class NotiAction {
    // 초대 알림용: 두 개의 버튼
    data class Invite(
        val acceptLabel: String = "수락",
        val rejectLabel: String = "거절"
    ) : NotiAction()

    // 더보기용: 텍스트 버튼만
    data class ShowMore(
        val count: Int
    ) : NotiAction()
}

data class NotificationFeedState(
    val recent: List<NotificationItem>,
    val past: List<NotificationItem>,
    val loading: Boolean = false,
)

@HiltViewModel
class NotificationViewModel @Inject constructor()
    : ViewModel() {
        private val _uiState = MutableStateFlow(
            NotificationFeedState(
                recent = listOf(
                    NotificationItem(
                        id = "r1",
                        title = "아직 열어보지 않은 카드가 있어요",
                        message = "잊기 전에 확인해보세요",
                        timeLabel = "지금",
                        type = NotiType.UnviewedReminder,
                        unread = true,
                    ),
                    NotificationItem(
                        id = "r2",
                        title = "새로운 카드가 생성 완료되었어요",
                        message = "",
                        timeLabel = "1시간 전",
                        type = NotiType.NewCard,
                        unread = true,
                        action = NotiAction.Invite()
                    ),
                    NotificationItem(
                        id = "r3",
                        title = "김친구 님이 '디자인' 보드를 공유하고 싶어해요",
                        message = "",
                        timeLabel = "1일 전",
                        type = NotiType.Invite,
                        unread = true,
                        action = NotiAction.Invite()),
                    ),
                past = emptyList(),
                loading = false
            )
        )
    val uiState: StateFlow<NotificationFeedState> = _uiState

    // Load or refresh from repository
    fun refresh() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(loading = true)
        // TODO fetch from repo
        _uiState.value = _uiState.value.copy(loading = false)
    }

    fun onClickItem(item: NotificationItem) {
        // TODO navigate or mark read
    }

    fun onClickPrimary(item: NotificationItem) {
        // 예시 초대 수락
        _uiState.value = _uiState.value.copy(
            recent = _uiState.value.recent.map {
                if (it.id == item.id) it.copy(unread = false) else it
            }
        )
        // TODO 서버 호출
    }

    fun onClickSecondary(item: NotificationItem) {
        // 예시 초대 거절
        _uiState.value = _uiState.value.copy(
            recent = _uiState.value.recent.filterNot { it.id == item.id }
        )
        // TODO 서버 호출
    }

    fun onClickMore() {
        // TODO navigate to filtered list or expand
    }
}

