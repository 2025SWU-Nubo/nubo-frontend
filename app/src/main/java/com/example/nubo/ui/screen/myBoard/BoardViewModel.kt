package com.example.nubo.ui.screen.myBoard

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.State
import com.example.nubo.data.model.BoardDeleteRequest
import com.example.nubo.data.model.BoardListItemResponse
import com.example.nubo.data.model.BoardRestoreRequest
import com.example.nubo.data.model.BoardSearchItemResponse
import com.example.nubo.data.model.CardDeleteRequest
import com.example.nubo.data.model.CardRestoreInfo
import com.example.nubo.data.model.CardRestoreRequest
import com.example.nubo.data.model.FavoriteRequest
import com.example.nubo.data.model.PagedResponse
import com.example.nubo.data.network.BoardService
import com.example.nubo.data.network.CardService
import com.example.nubo.data.repository.AuthRepository
import com.example.nubo.model.myBoard.BoardItem
import dagger.hilt.android.lifecycle.HiltViewModel
import getDisplayDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class CardDeleteEvent(
    val count: Int
)

@HiltViewModel
class BoardViewModel @Inject constructor(
    private val boardService: BoardService,
    private val cardService: CardService,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _boards = mutableStateOf<List<BoardItem>>(emptyList())
    val boards: State<List<BoardItem>> = _boards

    // Paging states
    private var page: Int = 0
    private var size: Int = 20
    private val _isLast = mutableStateOf(false)
    val isLast: State<Boolean> get() = _isLast
    private var sort: String = "LATEST"   // LATEST | OLDEST | ALPHABET
    private var filter: String = "ALL"    // ALL | FAVORITE | SHARED

    // 검색 결과 상태
    private val _searchResults = mutableStateOf<List<BoardItem>>(emptyList())
    val searchResults: State<List<BoardItem>> = _searchResults

    // 로딩 상태 (선택 사항)
    private val _isSearching = mutableStateOf(false)
    val isSearching: State<Boolean> = _isSearching

    // 필터 값 받아오기
    val currentFilter: String
        get() = filter

    // --- 토스트 메시지 상태 변수 ---
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage

    fun clearToastMessage() {
        _toastMessage.value = null
    }
    // ------------------------------------

    // --- 삭제/복구 관련 상태 및 이벤트 ---
    //  나의 보드 탭에서 보드/섹션/카드 동시 복구를 위한 ID 저장 변수
    private var lastDeletedBoardIdsForUndo: Set<Long> = emptySet()
    private var lastDeletedSectionIdsForUndo: Set<Long> = emptySet()
    private var lastDeletedCardRestoresForUndo: List<CardRestoreInfo> = emptyList()

    // 보드 삭제 시 사용된 옵션(deleteMode)을 저장할 변수
    private var lastDeletedBoardDeleteModeForUndo: String = ""

    // 최근 삭제 카드 ID 기억 (실행취소용)
    private var lastDeletedCardIds: Set<Int> = emptySet()
    private var lastCardDeleteMode: String = ""

    // "카드 삭제 완료" 이벤트 (스낵바/리스트 갱신 트리거)
    private val _cardDeleteCompleteEvent = MutableSharedFlow<Int>() // 삭제 개수 전달
    val cardDeleteCompleteEvent = _cardDeleteCompleteEvent.asSharedFlow()

    // --- 삭제 완료 시 액션 토스트를 띄우기 위한 이벤트 ---
    private val _deleteToastEvent = MutableSharedFlow<String>()
    val deleteToastEvent = _deleteToastEvent.asSharedFlow()

    // 페이징 로딩 상태
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> get() = _isLoading


    // Route 등에서 삭제가 완료되었음을 알릴 때 호출
    fun triggerDeleteToast(count: Int, isBoard: Boolean) {
        val message = if (isBoard) "${count}개의 보드를 삭제했어요." else "${count}개의 카드를 삭제했어요."
        viewModelScope.launch {
            _deleteToastEvent.emit(message)
        }
    }

    init {
        refresh()
    }

    /** Refresh from page 0 */
    fun refresh() {
        page = 0
        _isLast.value = false
        fetchBoards(reset = true)
    }

    /** Load next page if available */
    fun loadMore() {
        if ( _isLast.value) return
        fetchBoards(reset = false)
    }

    private fun fetchBoards(reset: Boolean) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val token = authRepository.getAccessToken()
                if (token.isNullOrBlank()) {
                    _boards.value = emptyList()
                    _isLast.value = true
                    return@launch
                }

                // call API with paging/sort/filter
                val res: PagedResponse<BoardListItemResponse> = boardService.getMyBoards(
                    acceptHeader = "application/json",
                    sort = sort,
                    filter = filter,
                    page = if (reset) 0 else page + 1,
                    size = size
                )

                // map DTOs → UI items
                val mapped = res.content.map { dto ->
                    BoardItem(
                        id = dto.id, // use stable id instead of index
                        serverBoardId = dto.id,
                        title = dto.name,
                        subtitle = "${dto.sectionCount} 섹션 ${dto.cardCount} 카드",
                        createdAt = getDisplayDate(dto.updatedAt),
                        source = dto.source,
                        imageUrl = dto.videoThumbnailUrl, // renamed field
                        isBookmarked = dto.favorite // 즐겨찾기 여부 매핑
                    )
                }
                _boards.value = if (reset) mapped else _boards.value + mapped

                // update paging flags
                page = res.number
                size = res.size
                _isLast.value = res.last
            } catch (e: Exception) {
                // 전체 초기 로딩일 때는 리스트 비우기
                if (reset) _boards.value = emptyList()
                Log.e("BoardViewModel", "Error fetching boards", e)

                // 보드 목록 불러오기 실패 시 토스트 메시지 설정
//                _toastMessage.value = "정보를 불러오지 못했어요"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Optional: expose sort/filter changes */
    fun setSort(newSort: String) {
        // must be one of LATEST/OLDEST/ALPHABET
        if (sort != newSort) {
            sort = newSort
            refresh()
        }
    }

    fun setFilter(newFilter: String) {
        // must be one of ALL/FAVORITE/SHARED
        if (filter != newFilter) {
            filter = newFilter
            refresh()
        }
    }

    // 즐겨찾기 토글 (낙관적 업데이트 + 실패 시 롤백)
    fun toggleFavorite(boardId: Int, currentFavorite: Boolean) {
        viewModelScope.launch {
            val before = _boards.value // rollback snapshot
            val newFav = !currentFavorite


            // 1) UI 먼저 반영 (빠른 피드백)
            _boards.value = before.map { b ->
                if (b.serverBoardId == boardId) b.copy(isBookmarked = newFav) else b
            }

            try {
                val token = authRepository.getAccessToken().orEmpty()
                // 2) 서버 PATCH 호출 (여기서만 Long으로 변환)
                boardService.setFavorite(
                    boardId = boardId.toLong(),
                    body = FavoriteRequest(favorite = newFav)
                )

                // 3) 성공 시 토스트 메세지
                val successMessage = if (newFav) {
                    "즐겨찾기가 완료되었어요."
                } else {
                    "즐겨찾기가 해제되었어요."
                }
                _toastMessage.value = successMessage

            } catch (e: Exception) {
                // 실패 시 rollback
                _boards.value = before
                Log.e("BoardViewModel", "toggleFavorite failed", e)
                _toastMessage.value = "즐겨찾기 변경에 실패했어요."
            }
        }
    }

    // 탭 전환 시 필터와 정렬 상태를 초기값으로 리셋하는 함수
    fun resetFilterAndSort() {
        sort = "LATEST"
        filter = "ALL"
        // 여기서 refresh()는 호출하지 않습니다.
        // (Route에서 reset 호출 후 바로 refresh()를 호출하기 때문)
    }

    // 상세 화면에서 보드명 수정 시 바로 적용
    fun applyRename(boardId: Int, newName: String) {
        _boards.value = _boards.value.map { b ->
            if (b.serverBoardId == boardId) b.copy(title = newName) else b
        }
    }

    // 보드 검색
    fun searchBoards(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            try {
                val token = authRepository.getAccessToken() ?: return@launch

                // 검색 API 호출
                val res: List<BoardSearchItemResponse> = boardService.searchBoards(
                    keyword = query,
                    sort = sort // ViewModel의 현재 정렬 상태를 재활용
                )

                // DTO -> UI 모델로 변환
                _searchResults.value = res.map { dto ->
                    BoardItem(
                        id = dto.id,
                        serverBoardId = dto.id,
                        title = dto.name,
                        subtitle = "${dto.sectionCount} 섹션 ${dto.cardCount} 카드",
                        createdAt = getDisplayDate(dto.updatedAt),
                        source = dto.source,
                        imageUrl = dto.videoThumbnailUrl,
                        isBookmarked = dto.favorite
                    )
                }
            } catch (e: Exception) {
                _searchResults.value = emptyList() // 에러 발생 시 결과 초기화
                Log.e("BoardViewModel", "Error searching boards", e)
            } finally {
                _isSearching.value = false
            }
        }
    }

    // 검색 결과 초기화
    fun clearSearch() {
        _searchResults.value = emptyList()
    }

    // --- 보드 전체 삭제 공통 로직 ---
    suspend fun deleteBoards(boardIds: Set<Int>): Int? {
        try {
            val token = "Bearer ${authRepository.getAccessToken()}"
            val request = BoardDeleteRequest(
                boardIds = boardIds.map { it.toLong() },
                deleteLinkedCards = "DELETE_ORPHANS"
            )
            // API 응답 타입이 List<BoardDeleteResponse>라고 가정합니다.
            val response = boardService.deleteBoards( request)

            return if (response.isSuccessful && response.body() != null) {
                val deletedItems = response.body()!!

                // 실행 취소를 위해 삭제된 ID들을 Long 타입 Set으로 저장
                lastDeletedBoardIdsForUndo = deletedItems.map { it.boardId }.toSet()
                lastDeletedSectionIdsForUndo = deletedItems.flatMap { it.deletedSectionIds }.toSet()

                // 'cardRestores'의 원본 리스트 구조를 그대로 저장
                lastDeletedCardRestoresForUndo = deletedItems.flatMap { it.cardRestores }

                // 응답에서 받은 삭제 옵션(option)을 저장
                lastDeletedBoardDeleteModeForUndo = deletedItems.firstOrNull()?.option ?: ""

                // 성공 시, 목록을 새로고침하고 삭제된 개수를 반환
                boardIds.size
            } else {
                Log.e("BoardViewModel", "Delete boards request failed: ${response.code()}")
                _toastMessage.value = "보드 삭제에 실패했습니다."
                null // 실패 시 null 반환
            }
        } catch (e: Exception) {
            Log.e("BoardViewModel", "Delete boards failed", e)
            _toastMessage.value = "삭제 중 오류가 발생했습니다."
            return null // 실패 시 null 반환
        }
    }

    // --- 보드 복구 로직 ---
    suspend fun undoLastDeletion() {
        // 1. 나의 보드 탭에서 '보드' 삭제를 취소하는 경우
        if (lastDeletedBoardIdsForUndo.isNotEmpty()) {
            try {
                val token = "Bearer ${authRepository.getAccessToken()}"

                // API 명세에 맞는 BoardRestoreRequest 객체를 생성
                val request = BoardRestoreRequest(
                    boardIds = lastDeletedBoardIdsForUndo.toList(),
                    sectionIds = lastDeletedSectionIdsForUndo.toList(),
                    // 'deleteBoards'에서 저장해둔 'cardRestores' 리스트를 그대로 전달
                    cardRestores = lastDeletedCardRestoresForUndo
                )

                val response = boardService.restoreBoards( request)

                if (response.isSuccessful && response.body() != null) {
                    // API 응답 스펙에 'restoredBoardIds'가 있으므로 사용
                    val count = response.body()!!.restoredBoardIds.size
                    _toastMessage.value = "${count}개의 보드 삭제가 취소되었어요."
                    refresh()
                } else {
                    _toastMessage.value = "삭제 실행 취소에 실패했어요."
                }
            } catch (e: Exception) {
                Log.e("BoardViewModel", "Undo board deletion failed", e)
                _toastMessage.value = "삭제 실행 취소에 실패했어요."
            } finally {
                // 성공/실패와 관계없이 임시 ID 초기화
                lastDeletedBoardIdsForUndo = emptySet()
                lastDeletedSectionIdsForUndo = emptySet()
                // 새로 만든 변수를 초기화
                lastDeletedCardRestoresForUndo = emptyList()
            }
            return // 보드 복구 로직 수행 후 함수 종료
        }

        // 2. 다른 화면에서 '카드'만 삭제했을 경우의 기존 복구 로직
        if (lastDeletedCardIds.isNotEmpty()) {
            try {
                val token = "Bearer ${authRepository.getAccessToken()}"
                val request = CardRestoreRequest(
                    cardIds = lastDeletedCardIds.map { it.toLong() },
                    boardId = null,
                    deleteMode = lastCardDeleteMode
                )
                val response = cardService.restoreCards( request)
                _toastMessage.value = "${response.restoredCount}개 카드 삭제가 취소되었어요."
                refresh() // 카드 목록도 새로고침이 필요할 수 있으므로 refresh() 호출
            } catch (e: Exception) {
                Log.e("BoardViewModel", "Undo card deletion failed", e)
                _toastMessage.value = "삭제 실행 취소에 실패했어요."
            } finally {
                lastDeletedCardIds = emptySet()
                lastCardDeleteMode = ""
            }
        }
    }

    // 전역(나의 카드)에서 카드만 삭제하는 함수 (boardId=null 로 요청)
    suspend fun deleteCardsFromGlobal(
        selectedCardIds: Set<Int>,
        deleteMode: String = "SOFT_DELETE"
    ): Boolean { // 2. Boolean 반환
        return try { // 3. try/catch가 함수 본체를 감싸도록
            // 액세스 토큰 준비
            val token = "Bearer ${authRepository.getAccessToken()}"

            // 카드 삭제 요청 바디 (boardId=null 이 핵심)
            val req = CardDeleteRequest(
                cardIds = selectedCardIds.map { it.toLong() },
                deleteMode = deleteMode
            )

            // 카드 삭제 API 호출
            val res = cardService.deleteCards( req)

            if (res.isSuccessful) {
                // 마지막 삭제 카드 기억 (실행취소 대응)
                lastDeletedCardIds = selectedCardIds
                lastCardDeleteMode = deleteMode

                // 여기서 액션 토스트 이벤트를 바로 트리거
                triggerDeleteToast(selectedCardIds.size, isBoard = false)

                // UI 에게 "N개 삭제됨" 알림
                _cardDeleteCompleteEvent.emit(selectedCardIds.size)
                true // 4. 성공 시 true 반환
            } else {
                _toastMessage.value = "카드 삭제에 실패했어요. (${res.code()})"
                false // 5. 실패 시 false 반환
            }
        } catch (t: Throwable) {
            _toastMessage.value = "카드 삭제 중 오류가 발생했어요."
            Log.e("BoardViewModel", "deleteCardsFromGlobal error", t)
            false // 6. 예외 시 false 반환
        }
    }
}
