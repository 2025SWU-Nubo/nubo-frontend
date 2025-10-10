package com.example.nubo.ui.screen.myBoard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.model.BoardDeleteRequest
import com.example.nubo.data.model.BoardRenameRequest
import com.example.nubo.data.model.BoardResponse
import com.example.nubo.data.model.BoardRestoreRequest
import com.example.nubo.data.model.BoardWithSectionsResponse
import com.example.nubo.data.model.FavoriteRequest
import com.example.nubo.data.network.BoardService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.nubo.data.repository.AuthRepository
import com.example.nubo.data.repository.BoardRepository
import com.example.nubo.data.model.UpsertBoardRequest
import com.example.nubo.data.model.BulkCopyRequest
import com.example.nubo.data.model.BulkMoveRequest
import com.example.nubo.data.model.CardDeleteRequest
import com.example.nubo.data.model.CardRestoreRequest
import com.example.nubo.data.network.CardService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class BoardDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val board: BoardResponse? = null,
    val favoriteOnly: Boolean = false,
    val page: Int = 0,
    val isLast: Boolean = false,
    val sort: String = "LATEST"
)

// --- 보드 트리 UI를 위한 데이터 클래스 ---
data class UiBoardNode(
    val id: Long,
    val title: String,
    val children: List<UiBoardNode> = emptyList(),
)
// -----------------------------------------


@HiltViewModel
class BoardDetailViewModel @Inject constructor(
    private val boardRepository: BoardRepository,
    private val authRepository: AuthRepository,
    private val boardService: BoardService,
    private val cardService: CardService
) : ViewModel() {
    private val _ui = MutableStateFlow(BoardDetailUiState())
    val ui: StateFlow<BoardDetailUiState> = _ui

    private var currentBoardId: Int = -1
    private var bootstrapped = false
    private val pageSize = 20

    // 삭제 성공 시 UI에 신호를 보내기 위한 SharedFlow
    private val _deleteCompleteEvent = MutableSharedFlow<Unit>()
    val deleteCompleteEvent = _deleteCompleteEvent.asSharedFlow()

    //  마지막으로 삭제된 항목들의 ID를 임시 저장하는 변수
    private var lastDeletedSectionIds: Set<Int> = emptySet()
    private var lastDeletedCardIds: Set<Int> = emptySet()

    //  토스트 메시지 상태 변수와 초기화 함수
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    fun init(boardId: Int) {
        if (bootstrapped && currentBoardId == boardId) return
        bootstrapped = true
        currentBoardId = boardId
        _ui.value = BoardDetailUiState(isLoading = true, favoriteOnly = false)
        loadPage(reset = true)
    }

    // 즐겨찾기 필터
    fun setFavoriteFilter(enabled: Boolean) {
        if (_ui.value.favoriteOnly == enabled) return
        _ui.value = _ui.value.copy(favoriteOnly = enabled)
        loadPage(reset = true)
    }

    // 정렬
    fun setSort(sortKey: String) {
        // 이미 같은 정렬 옵션이면 아무것도 하지 않음
        if (_ui.value.sort == sortKey) return

        // 새로운 정렬 값으로 상태를 업데이트하고, 페이지를 처음부터 다시 로드
        _ui.value = _ui.value.copy(sort = sortKey)
        loadPage(reset = true)
    }

    fun loadNextPage() {
        val s = _ui.value
        if (s.isLoading || s.isLast) return
        loadPage(reset = false)
    }

    // 페이지 조회
    private fun loadPage(reset: Boolean) {
        val s = _ui.value
        val targetPage = if (reset) 0 else s.page + 1
        _ui.value = s.copy(isLoading = true, error = null)

        viewModelScope.launch {
            val token = authRepository.getAccessToken().orEmpty()
            boardRepository.getBoardDetail(
                token = token,
                boardId = currentBoardId,
                favoriteOnly = _ui.value.favoriteOnly,
                sort = _ui.value.sort,
                page = targetPage,
                size = pageSize
            ).onSuccess { resp ->
                val merged = if (reset || s.board == null) {
                    resp
                } else {
                    val prev = s.board!!
                    val newContent = prev.cards.content + resp.cards.content
                    resp.copy(cards = resp.cards.copy(content = newContent))
                }
                _ui.value = s.copy(
                    isLoading = false,
                    board = merged,
                    page = resp.cards.number,
                    isLast = resp.cards.last
                )
            }.onFailure { e ->
                _ui.value = s.copy(
                    isLoading = false,
                    error = e.message ?: "불러오기에 실패했습니다."
                )
            }
        }
    }

    // 즐겨찾기 설정
    fun toggleSectionFavorite(sectionId: Int, currentFavorite: Boolean) {
        val beforeState = _ui.value
        val board = beforeState.board ?: return
        val newFav = !currentFavorite

        // 1) UI를 즉시 업데이트하여 사용자에게 빠른 피드백
        val updatedSections = board.sections.map { sec ->
            if (sec.id.toInt() == sectionId) sec.copy(favorite = newFav) else sec
        }
        _ui.value = beforeState.copy(board = board.copy(sections = updatedSections))

        // 2) 코루틴을 사용해 백그라운드에서 서버 API를 호출
        viewModelScope.launch {
            try {
                val token = authRepository.getAccessToken().orEmpty()
                // 보내주신 API 명세서와 동일한 boardService.setFavorite 함수를 호출
                boardService.setFavorite(
                    authHeader = "Bearer $token",
                    boardId = sectionId.toLong(), // 섹션 ID를 전달
                    body = FavoriteRequest(favorite = !currentFavorite)
                )
                // 성공하면 변경된 UI 상태가 유지
            } catch (t: Throwable) {
                // 3) 만약 API 호출이 실패하면, 원래 상태로 UI를 롤백
                _ui.value = beforeState
            }
        }
    }

    // 섹션 생성
    fun createSection(name: String) {
        val parentShared = _ui.value.board?.shared ?: false          // 부모 보드 공유 여부 따라감

        viewModelScope.launch {
            // 로딩 표시
            _ui.value = _ui.value.copy(isLoading = true, error = null)
            try {
                val token = "Bearer ${authRepository.getAccessToken()}"
                val req = UpsertBoardRequest(
                    name = name,                      // 섹션 이름
                    boardType = "SECTION",            // 스펙상 문자열이면 그대로 사용
                    source = "USER",  // 보드 생성 출처 고정
                    shared = parentShared,             // 부모와 동일하게
                    favorite = false,
                    memberEmails = null,
                    parentBoardId = currentBoardId.toLong()
                )
                boardService.upsertBoard(
                    body = req,
                    authHeader = token
                )
                // 성공 후 상세 재조회
                loadPage(reset = true)
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(
                    isLoading = false,
                    error = t.message ?: "섹션 생성에 실패했어요"
                )
            }
        }
    }

    // 섹션 이름 변경
    fun renameSection(sectionId: Int, newName: String) {
        val before = _ui.value
        val detail = before.board ?: return

        // UI 즉시 반영
        val updatedSections = detail.sections.map { s ->
            if (s.id.toInt() == sectionId) s.copy(name = newName) else s
        }
        _ui.value = before.copy(board = detail.copy(sections = updatedSections))

        // 서버 PATCH
        viewModelScope.launch {
            try {
                val token = "Bearer ${authRepository.getAccessToken()}"
                boardService.renameBoardOrSection(
                    authHeader = token,
                    boardId = sectionId.toLong(),
                    body = BoardRenameRequest(name = newName)
                )
            } catch (t: Throwable) {
                // 실패 시 롤백
                _ui.value = before
            }
        }
    }

    // 현재 보드 또는 섹션 이름 변경
    fun renameCurrentBoard(newName: String) {
        val before = _ui.value
        val detail = before.board ?: return
        val boardId = currentBoardId.takeIf { it > 0 } ?: return

        // UI 즉시 반영
        val patched = detail.copy(
            // 프로젝트 모델에 맞춰 필드명 지정
            // name 또는 boardName 등 실제 필드로 교체
            name = newName
        )
        _ui.value = before.copy(board = patched)

        viewModelScope.launch {
            try {
                val token = "Bearer ${authRepository.getAccessToken()}"
                boardService.renameBoardOrSection(
                    authHeader = token,
                    boardId = boardId.toLong(),
                    body = BoardRenameRequest(name = newName)
                )
            } catch (t: Throwable) {
                _ui.value = before
            }
        }
    }

    // --- 보드 트리 로딩 상태 ---
    sealed class BoardsState {
        data object Idle : BoardsState()
        data object Loading : BoardsState()
        data class Loaded(val boards: List<UiBoardNode>) : BoardsState()
        data class Error(val message: String) : BoardsState()
    }

    private val _boards = MutableStateFlow<BoardsState>(BoardsState.Idle)
    val boards: StateFlow<BoardsState> = _boards
    // ------------------------------------

    // --- 보드 목록 로딩 함수 ---
    fun loadBoards() {
        if (_boards.value is BoardsState.Loading || _boards.value is BoardsState.Loaded) return

        viewModelScope.launch {
            _boards.value = BoardsState.Loading
            try {
                val token = "Bearer ${authRepository.getAccessToken()}"
                // videoService 대신 boardService를 사용하도록 수정
                val res = boardService.getBoardsWithSections(token)

                if (res.isSuccessful) {
                    val body = res.body().orEmpty()
                    _boards.value = BoardsState.Loaded(body.toUiNodes())
                } else {
                    _boards.value = BoardsState.Error("서버 오류: ${res.code()}")
                }
            } catch (e: Exception) {
                _boards.value = BoardsState.Error("네트워크 오류: ${e.localizedMessage ?: "알 수 없음"}")
            }
        }
    }

    // 서버 DTO → UI 노드 변환 헬퍼 함수
    private fun List<BoardWithSectionsResponse>.toUiNodes(): List<UiBoardNode> {
        return map { board ->
            UiBoardNode(
                id = board.id,
                title = board.name,
                children = board.sections.map { sec ->
                    UiBoardNode(id = sec.id, title = sec.name)
                }
            )
        }
    }
    // -----------

    // 복제 함수
    fun copySelectedItems(
        targetBoardId: Long,
        selectedSectionIds: Set<Int>,
        selectedCardIds: Set<Int>
    ) {

        Log.d("CopyAPI", "--- copySelectedItems 함수 시작 ---")
        Log.d("CopyAPI", "Target: $targetBoardId, Sections: $selectedSectionIds, Cards: $selectedCardIds")
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true, error = null)
            try {
                val token = "Bearer ${authRepository.getAccessToken()}"
                val sourceBoardId = currentBoardId.toLong()

                val request = BulkCopyRequest(
                    targetBoardId = targetBoardId,
                    // 선택된 ID가 없으면 null, 있으면 Long 타입 리스트로 변환
                    boardIds = selectedSectionIds.takeIf { it.isNotEmpty() }?.map { it.toLong() },
                    cardIds = selectedCardIds.takeIf { it.isNotEmpty() }?.map { it.toLong() }
                )

                val response = boardService.bulkCopy(
                    authHeader = token,
                    sourceBoardId = sourceBoardId,
                    body = request
                )

                if (response.isSuccessful) {
                    // 성공 시, 현재 화면을 새로고침하여 변경사항을 반영
                    Log.i("CopyAPI", "성공: ${response.body()}")
                    _toastMessage.value = "복제가 완료되었습니다!"
                    loadPage(reset = true)
                } else {
                    // API 에러 처리 (예: 공유 보드에 복사 시도 등)
                    _ui.value = _ui.value.copy(isLoading = false, error = "복사에 실패했습니다: ${response.code()}")
                }
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(isLoading = false, error = "네트워크 오류: ${e.message}")
            }
        }
    }

    // 이동 함수
    fun moveSelectedItems(
        targetBoardId: Long,
        selectedSectionIds: Set<Int>,
        selectedCardIds: Set<Int>
    ) {
        Log.d("MoveAPI", "--- moveSelectedItems 함수 시작 ---")
        Log.d("MoveAPI", "Target: $targetBoardId, Sections: $selectedSectionIds, Cards: $selectedCardIds")

        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true, error = null)
            try {
                val token = "Bearer ${authRepository.getAccessToken()}"
                val sourceBoardId = currentBoardId.toLong()

                val request = BulkMoveRequest(
                    targetBoardId = targetBoardId,
                    boardIds = selectedSectionIds.takeIf { it.isNotEmpty() }?.map { it.toLong() },
                    cardIds = selectedCardIds.takeIf { it.isNotEmpty() }?.map { it.toLong() }
                )

                Log.d("MoveAPI", "Request Body 생성: $request")
                val response = boardService.bulkMove(
                    authHeader = token,
                    sourceBoardId = sourceBoardId,
                    body = request
                )

                if (response.isSuccessful) {
                    Log.i("MoveAPI", "성공: ${response.body()}")
                    _toastMessage.value = "이동이 완료되었습니다!"
                    loadPage(reset = true) // 이동 후 현재 화면은 아이템이 사라졌으므로 새로고침
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("MoveAPI", "실패: Code=${response.code()}, ErrorBody=${errorBody}")
                    _ui.value = _ui.value.copy(isLoading = false, error = "이동에 실패했습니다: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("MoveAPI", "네트워크 예외 발생", e)
                _ui.value = _ui.value.copy(isLoading = false, error = "오류 발생: ${e.message}")
            }
        }
    }

    // '보드에서 제거' (Detach) 함수
    suspend fun removeItemsFromBoard(selectedSectionIds: Set<Int>, selectedCardIds: Set<Int>) {
        // API 명세에 따라 옵션값 설정
        val boardDeleteOption = "DETACH_ONLY"
        val cardDeleteOption = "DETACH_ONLY"
        executeDeleteActions(selectedSectionIds, selectedCardIds, boardDeleteOption, cardDeleteOption)
    }

    // '영구 삭제' (Soft Delete) 함수
    suspend fun deleteItems(selectedSectionIds: Set<Int>, selectedCardIds: Set<Int>) {
        // API 명세에 따라 옵션값 설정
        val boardDeleteOption = "DELETE_ORPHANS" // 삭제 시 하위 카드도 삭제
        val cardDeleteOption = "SOFT_DELETE"
        executeDeleteActions(selectedSectionIds, selectedCardIds, boardDeleteOption, cardDeleteOption)
    }

    // 공통 삭제 로직
    private suspend fun executeDeleteActions(
        selectedSectionIds: Set<Int>,
        selectedCardIds: Set<Int>,
        boardDeleteOption: String,
        cardDeleteOption: String
    ) {
        // --- 삭제 실행 전에 ID 저장 ---
        lastDeletedSectionIds = selectedSectionIds
        lastDeletedCardIds = selectedCardIds
        // --------------------------
        _ui.value = _ui.value.copy(isLoading = true, error = null)
        try {
            val token = "Bearer ${authRepository.getAccessToken()}"
            var allSuccess = true

            // 코루틴 스코프를 만들어 섹션과 카드 삭제를 병렬로 처리
            coroutineScope {
                // 섹션 삭제 API 호출 (선택된 경우에만)
                val boardJob = if (selectedSectionIds.isNotEmpty()) {
                    async {
                        val request = BoardDeleteRequest(
                            boardIds = selectedSectionIds.map { it.toLong() },
                            deleteLinkedCards = boardDeleteOption
                        )
                        val response = boardService.deleteBoards(token, request)
                        response.isSuccessful
                    }
                } else null

                // 카드 삭제 API 호출 (선택된 경우에만)
                val cardJob = if (selectedCardIds.isNotEmpty()) {
                    async {
                        val request = CardDeleteRequest(
                            cardIds = selectedCardIds.map { it.toLong() },
                            deleteMode = cardDeleteOption
                        )
                        val response = cardService.deleteCards(token, request)
                        // 응답이 성공했고, 모든 카드의 상태가 DELETED 또는 DETACHED인지 확인
                        response.isSuccessful && response.body()?.results?.all {
                            it.status == "DELETED" || it.status == "DETACHED"
                        } ?: false
                    }
                } else null

                // 모든 작업의 성공 여부를 확인
                val boardResult = boardJob?.await() ?: true
                val cardResult = cardJob?.await() ?: true
                allSuccess = boardResult && cardResult
            }

            if (allSuccess) {
                _deleteCompleteEvent.emit(Unit) // UI에 성공 신호 전송
                loadPage(reset = true) // 화면 새로고침
            } else {
                throw Exception("일부 항목 삭제에 실패했습니다.")
            }
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(isLoading = false, error = e.message ?: "삭제 중 오류가 발생했습니다.")
            // 실패 시 임시 ID 초기화
            lastDeletedSectionIds = emptySet()
            lastDeletedCardIds = emptySet()
        }
    }

    // 삭제 실행 취소(복구) 함수
    fun undoLastDeletion() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true, error = null)
            try {
                val token = "Bearer ${authRepository.getAccessToken()}"
                var restoredSections = 0
                var restoredCards = 0

                coroutineScope {
                    // 섹션 복구 API 호출 (임시 저장된 ID가 있을 경우)
                    val boardJob = if (lastDeletedSectionIds.isNotEmpty()) {
                        async {
                            val request = BoardRestoreRequest(boardIds = lastDeletedSectionIds.map { it.toLong() })
                            boardService.restoreBoards(token, request).restoredCount
                        }
                    } else null

                    // 카드 복구 API 호출 (임시 저장된 ID가 있을 경우)
                    val cardJob = if (lastDeletedCardIds.isNotEmpty()) {
                        async {
                            val request = CardRestoreRequest(cardIds = lastDeletedCardIds.map { it.toLong() })
                            cardService.restoreCards(token, request).restoredCount
                        }
                    } else null

                    restoredSections = boardJob?.await() ?: 0
                    restoredCards = cardJob?.await() ?: 0
                }

                // 복구 결과에 따라 토스트 메시지 생성
                val message = when {
                    restoredSections > 0 && restoredCards > 0 -> "${restoredSections}개의 섹션과 ${restoredCards}개의 카드 삭제가 취소되었습니다."
                    restoredSections > 0 -> "${restoredSections}개의 섹션 삭제가 취소되었습니다."
                    restoredCards > 0 -> "${restoredCards}개의 카드 삭제가 취소되었습니다."
                    else -> null
                }

                message?.let { _toastMessage.value = it }
                loadPage(reset = true) // 화면 새로고침

            } catch (e: Exception) {
                _ui.value = _ui.value.copy(isLoading = false, error = e.message ?: "복구 중 오류가 발생했습니다.")
            } finally {
                // 작업 완료 후 임시 ID 초기화
                lastDeletedSectionIds = emptySet()
                lastDeletedCardIds = emptySet()
            }
        }
    }
}
