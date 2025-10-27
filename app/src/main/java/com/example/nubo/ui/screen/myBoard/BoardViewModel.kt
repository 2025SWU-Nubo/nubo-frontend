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
import com.example.nubo.data.model.BulkCopyRequest
import com.example.nubo.data.model.BulkMoveRequest
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject


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
    private var isLast: Boolean = false
    private var sort: String = "LATEST"   // LATEST | OLDEST | ALPHABET
    private var filter: String = "ALL"    // ALL | FAVORITE | SHARED

    // 검색 결과 상태
    private val _searchResults = mutableStateOf<List<BoardItem>>(emptyList())
    val searchResults: State<List<BoardItem>> = _searchResults

    // 로딩 상태 (선택 사항)
    private val _isSearching = mutableStateOf(false)
    val isSearching: State<Boolean> = _isSearching

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

    // 다른 화면에서 카드만 삭제/복구할 때 사용되는 변수
    private var lastDeletedCardIds: Set<Int> = emptySet()
    private var lastCardDeleteMode: String = ""

    // 보드 삭제 시 사용된 옵션(deleteMode)을 저장할 변수
    private var lastDeletedBoardDeleteModeForUndo: String = ""


    init {
        refresh()
    }

    /** Refresh from page 0 */
    fun refresh() {
        page = 0
        isLast = false
        fetchBoards(reset = true)
    }

    /** Load next page if available */
    fun loadMore() {
        if (isLast) return
        fetchBoards(reset = false)
    }

    private fun fetchBoards(reset: Boolean) {
        viewModelScope.launch {
            try {
                val token = authRepository.getAccessToken()
                if (token.isNullOrBlank()) {
                    _boards.value = emptyList()
                    isLast = true
                    return@launch
                }

                // call API with paging/sort/filter
                val res: PagedResponse<BoardListItemResponse> = boardService.getMyBoards(
                    authHeader = "Bearer $token",
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
                isLast = res.last
            } catch (e: Exception) {
                if (reset) _boards.value = emptyList()
                Log.e("BoardViewModel", "Error fetching boards", e)
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
            val before = _boards.value // 롤백 스냅샷

            // 1) UI 먼저 반영 (빠른 피드백)
            _boards.value = before.map { b ->
                // 한글 주석: Int끼리 비교 (오류 없음)
                if (b.serverBoardId == boardId) b.copy(isBookmarked = !currentFavorite) else b
            }

            try {
                val token = authRepository.getAccessToken().orEmpty()
                // 2) 서버 PATCH 호출 (여기서만 Long으로 변환)
                boardService.setFavorite(
                    authHeader = "Bearer $token",
                    boardId = boardId.toLong(),                    // ← Int → Long
                    body = FavoriteRequest(favorite = !currentFavorite)
                )
                // 성공 시 그대로 유지
            } catch (e: Exception) {
                // 실패 시 롤백
                _boards.value = before
                Log.e("BoardViewModel", "toggleFavorite failed", e)
            }
        }
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
                    authHeader = "Bearer $token",
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

    // --- '나의 카드' 탭 전용 복제 함수 ---
    fun copyCardsFromGlobal(targetBoardId: Long, selectedCardIds: Set<Int>) {
        viewModelScope.launch {
            try {
                val token = "Bearer ${authRepository.getAccessToken()}"
                val request = BulkCopyRequest(
                    targetBoardId = targetBoardId,
                    boardIds = null,
                    cardIds = selectedCardIds.map { it.toLong() }
                )
                val response = boardService.bulkCopyFromRoot(
                    authHeader = token,
                    body = request
                )
                if (response.isSuccessful) {
                    _toastMessage.value = "${selectedCardIds.size}개의 카드가 복제되었습니다."
                } else {
                    Log.e("BoardViewModel", "Global Card Copy failed: ${response.code()}")
                    _toastMessage.value = "카드 복제에 실패했습니다."
                }
            } catch (e: Exception) {
                Log.e("BoardViewModel", "Global Card Copy network error", e)
                _toastMessage.value = "카드 복제 중 오류가 발생했습니다."
            }
        }
    }

    // --- '나의 카드' 탭 전용 이동 함수 ---
    fun moveCardsFromGlobal(targetBoardId: Long, selectedCardIds: Set<Int>) {
        viewModelScope.launch {
            try {
                val token = "Bearer ${authRepository.getAccessToken()}"
                val request = BulkMoveRequest(
                    targetBoardId = targetBoardId,
                    boardIds = null,
                    cardIds = selectedCardIds.map { it.toLong() }
                )
                val response = boardService.bulkMoveFromRoot(
                    authHeader = token,
                    body = request
                )
                if (response.isSuccessful) {
                    _toastMessage.value = "${selectedCardIds.size}개의 카드가 이동되었습니다."
                } else {
                    Log.e("BoardViewModel", "Global Card Move failed: ${response.code()}")
                    _toastMessage.value = "카드 이동에 실패했습니다."
                }
            } catch (e: Exception) {
                Log.e("BoardViewModel", "Global Card Move network error", e)
                _toastMessage.value = "카드 이동 중 오류가 발생했습니다."
            }
        }
    }

    // --- 보드 이름 변경 API 호출 함수 ---
    fun renameBoard(boardId: Int, newName: String) {
        // applyRename으로 UI는 즉시 변경되었으므로, 여기서는 API 호출만 수행
        viewModelScope.launch {
            try {
                val token = "Bearer ${authRepository.getAccessToken()}"
                boardService.renameBoardOrSection(
                    authHeader = token,
                    boardId = boardId.toLong(),
                    body = com.example.nubo.data.model.BoardRenameRequest(name = newName) // BoardRenameRequest 임포트 필요
                )
            } catch (t: Throwable) {
                // 실패 시 UI 롤백이 필요하다면 여기에 로직 추가 (현재는 생략)
                Log.e("BoardViewModel", "renameBoard failed", t)
                _toastMessage.value = "이름 변경에 실패했습니다."
            }
        }
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
            val response = boardService.deleteBoards(token, request)

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

                val response = boardService.restoreBoards(token, request)

                if (response.isSuccessful && response.body() != null) {
                    // API 응답 스펙에 'restoredBoardIds'가 있으므로 사용
                    val count = response.body()!!.restoredBoardIds.size
                    _toastMessage.value = "${count}개의 보드 삭제가 취소되었습니다."
                    // 또는 전체 복구 개수를 사용
                    // val totalCount = response.body()!!.restoredCount
                    // _toastMessage.value = "${totalCount}개 항목이 복구되었습니다."

                    refresh()
                } else {
                    _toastMessage.value = "복구에 실패했습니다."
                }
            } catch (e: Exception) {
                Log.e("BoardViewModel", "Undo board deletion failed", e)
                _toastMessage.value = "복구 중 오류가 발생했습니다."
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
                val response = cardService.restoreCards(token, request)
                _toastMessage.value = "${response.restoredCount}개 카드 삭제가 취소되었습니다."
                refresh() // 카드 목록도 새로고침이 필요할 수 있으므로 refresh() 호출
            } catch (e: Exception) {
                Log.e("BoardViewModel", "Undo card deletion failed", e)
                _toastMessage.value = "복구 중 오류가 발생했습니다."
            } finally {
                lastDeletedCardIds = emptySet()
                lastCardDeleteMode = ""
            }
        }
    }
}
