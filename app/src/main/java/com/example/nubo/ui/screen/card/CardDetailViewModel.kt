package com.example.nubo.ui.screen.card

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.components.toast.buildHighlightedTitle
import com.example.nubo.data.model.CardDetailResponse
import com.example.nubo.data.model.CardFavoriteRequest
import com.example.nubo.data.repository.AuthRepository
import com.example.nubo.data.repository.CardRepository
import com.example.nubo.di.UserProgressEventHolder
import com.example.nubo.model.card.CardDetailItem
import com.example.nubo.ui.theme.PurpleMain500
import dagger.hilt.android.lifecycle.HiltViewModel
import formatIsoDateToDisplayLegacy
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.await

// UI state sealed class
sealed interface CardDetailUiState {
    data object Loading : CardDetailUiState
    data class Success(val item: CardDetailItem) : CardDetailUiState
    data class Error(val message: String) : CardDetailUiState
}

// 정보 아이콘 클릭시, 말풍선
sealed interface InfoUiState {
    data object Hidden : InfoUiState
    data object Visible : InfoUiState
}


@HiltViewModel
class CardDetailViewModel @Inject constructor(
    private val repository: CardRepository,
    private val authRepository: AuthRepository,
    private val eventHolder: UserProgressEventHolder,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Read required nav arg
    private val cardId: Int = checkNotNull(savedStateHandle["cardId"])

    private val _uiState = MutableStateFlow<CardDetailUiState>(CardDetailUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast = _toast.asStateFlow()

    // 레벨업 / 누베리 수확 토스트
    private val _toast2 = MutableStateFlow<Pair<AnnotatedString, String>?>(null)
    val toast2 = _toast2.asStateFlow()

    private val _infoState = MutableStateFlow<InfoUiState>(InfoUiState.Hidden)
    val infoState = _infoState.asStateFlow()

    // Load card detail by id
     private fun load() {
        viewModelScope.launch {
            _uiState.value = CardDetailUiState.Loading

            val token = authRepository.getAccessToken()
            if (token.isNullOrBlank()) {
                _uiState.value = CardDetailUiState.Error("인증 토큰이 없습니다. 다시 로그인해주세요.")
                return@launch
            }
            runCatching {
                // ⚠️ Replace with your actual API call
                val res: CardDetailResponse = repository.getCardDetail(token, cardId).await()
                res.toUi()
            }.onSuccess { ui ->
                _uiState.value = CardDetailUiState.Success(ui)

                // 레벨업 또는 열매 획득 시 토스트 메시지 생성
                if (ui.stageUp) {
                    val title = buildHighlightedTitle(
                        full = "Level Up!",
                        highlight = "Level Up!",
                        highlightColor = Color.White
                    )
                    val summary = "성장보드에서 확인해보세요."
                    _toast2.value = Pair(title, summary)

                    // LearnScreen 애니메이션을 위해 EventHolder에 저장
                    eventHolder.postLevelUpEvent(ui.stage)
                } else if (ui.berryGained) {
                    val title = buildHighlightedTitle(
                        full = "누베리 성장 완료!",
                        highlight = "누베리 성장 완료!",
                        highlightColor = Color.White
                    )
                    val summary = "성장보드에서 수확해보세요."
                    _toast2.value = Pair(title, summary)

                    // LearnScreen 알림을 위해 EventHolder에 저장
                    eventHolder.postBerryGainedEvent()
                }
            }.onFailure { e ->
                val msg = when (e) {
                is HttpException -> when (e.code()) {
                    401, 403 -> "세션이 만료되었어요.\n다시 로그인해주세요."
                    404 -> "해당 카드를 찾을 수 없습니다."
                    else -> "서버 오류(${e.code()})가 발생했습니다."
                }
                else -> e.message ?: "알 수 없는 오류가 발생했습니다."
            }
                _uiState.value = CardDetailUiState.Error(msg)
            }
        }
    }


    // 요약 노트 수정 요약 노트 리프레쉬 함수
    fun refresh() { load() }

    fun toggleFavorite(){
        val current = _uiState.value
        if(current !is CardDetailUiState.Success) return
        viewModelScope.launch {
            val token = authRepository.getAccessToken() ?: return@launch
            val old = current.item
            val next = !old.isFavorite

            _uiState.value = CardDetailUiState.Success(old.copy(isFavorite = next))

            runCatching {
                // 즐겨찾기 업데이트
                val res = repository.updateFavorite(
                    token = token,
                    cardId = old.cardId,
                    body = CardFavoriteRequest(favorite = next)
                ).await()

//                _toast.value = if(next)"즐겨 찾기 완료!" else "즐겨 찾기 해제!"

                val confirmed = res.favorite
                _uiState.value = CardDetailUiState.Success(old.copy(isFavorite = confirmed))
            }.onFailure{e ->
                /* 4 실패  되돌리기 가능 상태 유지  에러 토스트 */
                _toast.value = e.humanMessage()
                _uiState.value = CardDetailUiState.Success(old)
            }
        }
    }

    fun consumeToast() { _toast.value = null }

    // 레벨업, 누베리 획득 토스트를 초기화하는 함수
    fun consumeToast2() { _toast2.value = null }

    // 정보 말풍선
    fun showInfoBubble() {
        // Only show when we have data
        if (_uiState.value is CardDetailUiState.Success) {
            _infoState.value = InfoUiState.Visible
        }
    }

    fun hideInfoBubble() {
        _infoState.value = InfoUiState.Hidden
    }

}

private fun Throwable.humanMessage(): String = when (this) {
    is HttpException -> "서버 오류(${code()})가 발생했습니다"
    else -> message ?: "다시 시도 해주세요."
}

