package com.example.nubo.ui.screen.myBoard

import android.util.Log
import androidx.lifecycle.SavedStateHandle
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
import com.example.nubo.data.model.CardRestoreInfo
import com.example.nubo.data.network.CardService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.example.nubo.utils.refreshTicks
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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
    private val savedStateHandle: SavedStateHandle,
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

    // --- 삭제 성공 시 '삭제된 개수(Int)'를 UI에 보내기 위한 SharedFlow ---
    private val _deleteCompleteEvent = MutableSharedFlow<Int>()
    val deleteCompleteEvent = _deleteCompleteEvent.asSharedFlow()

    //  마지막으로 삭제된 항목들의 ID를 임시 저장하는 변수
    private var lastDeletedSectionIds: Set<Long> = emptySet()
    private var lastDeletedCardIds: Set<Long> = emptySet()
    // --- 마지막 카드 삭제 유형을 저장하는 변수 ---
    private var lastCardDeleteMode: String = ""
    // '섹션 삭제' 시 함께 삭제된 카드 정보 (boardId + cardIds)
    private var lastDeletedCardRestores: List<CardRestoreInfo> = emptyList()

    //  토스트 메시지 상태 변수와 초기화 함수
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    // --- ViewModel 최초 생성 시 신호 수신 시작 ---
    init {
        observeRefreshSignal() // [추가]
    }

    // --- 새로고침 신호(tick)를 수신하는 함수 ---
    private fun observeRefreshSignal() {
        savedStateHandle.refreshTicks()
            .onEach { tick ->
                // 0L은 초기값이므로 무시하고,
                // currentBoardId가 세팅된 이후에 수신된 신호일 때만 새로고침
                if (tick != 0L && currentBoardId != -1) {
                    Log.d("RefreshSignal", "Tick received in BoardDetailViewModel, refreshing $currentBoardId")
                    // init() 또는 loadPage(reset=true)를 호출해 데이터를 새로고침
                    loadPage(reset = true)
                }
            }
            .launchIn(viewModelScope)
    }

    fun init(boardId: Int, forceRefresh: Boolean = false) {

        // 강제 새로고침이 아닐 때만 이 보호 로직을 실행
        if (!forceRefresh && bootstrapped && currentBoardId == boardId) {
            Log.d("BoardDetailVM", "Init skipped (already loaded)")
            return
        }

        Log.d("BoardDetailVM", "Init executing (Force: $forceRefresh)")
        bootstrapped = true
        currentBoardId = boardId

        // 기존 필터/정렬 상태를 유지하도록 UiState를 완전 리셋하는 대신 copy 사용
        _ui.value = _ui.value.copy(
            isLoading = true,
            error = null
        )
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
                    // --- 성공 시, 개수를 포함한 토스트 메시지를 설정 ---
                    val count = selectedSectionIds.size + selectedCardIds.size
                    _toastMessage.value = "${count}개의 항목 복제가 완료되었습니다."
                    // --- BoardDetailScreen일 때만 새로고침 ---
                    if (currentBoardId > -1) {
                        loadPage(reset = true)
                    }
                } else {
                    // 실패 시 로그 및 토스트
                    val errorBody = response.errorBody()?.string()
                    Log.e("BoardDetailVM", "Copy failed: Code=${response.code()}, Body=$errorBody")
                    _toastMessage.value = "복제에 실패했습니다."
                    _ui.value = _ui.value.copy(isLoading = false, error = "복제에 실패했습니다: ${response.code()}")
                }
            } catch (e: Exception) {
                // 네트워크 오류 시 로그 및 토스트
                Log.e("BoardDetailVM", "Copy network error", e)
                _toastMessage.value = "복제 중 오류가 발생했습니다."
                _ui.value = _ui.value.copy(isLoading = false, error = "복제 네트워크 오류: ${e.message}")
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
                    // --- 성공 시, 개수를 포함한 토스트 메시지를 설정 ---
                    val count = selectedSectionIds.size + selectedCardIds.size
                    _toastMessage.value = "${count}개의 항목 이동이 완료되었습니다."
                    // --- BoardDetailScreen일 때만 새로고침 ---
                    if (currentBoardId > -1) {
                        loadPage(reset = true)
                    }
                } else {
                    // 실패 시 로그 및 토스트
                    val errorBody = response.errorBody()?.string()
                    Log.e("BoardDetailVM", "Move failed: Code=${response.code()}, Body=$errorBody")
                    _toastMessage.value = "이동에 실패했습니다."
                    _ui.value = _ui.value.copy(isLoading = false, error = "이동에 실패했습니다: ${response.code()}")
                }
            } catch (e: Exception) {
                // 네트워크 오류 시 로그 및 토스트
                Log.e("BoardDetailVM", "Move network error", e)
                _toastMessage.value = "이동 중 오류가 발생했습니다."
                _ui.value = _ui.value.copy(isLoading = false, error = "이동 오류 발생: ${e.message}")
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

    // --- 공통 삭제 로직 (응답값 처리 추가) ---
    private suspend fun executeDeleteActions(
        selectedSectionIds: Set<Int>,
        selectedCardIds: Set<Int>,
        boardDeleteOption: String,
        cardDeleteOption: String
    ) {
        // 삭제 실행 전 임시 변수 초기화
        lastDeletedSectionIds = emptySet()
        lastDeletedCardIds = emptySet()
        lastDeletedCardRestores = emptyList()
        lastCardDeleteMode = cardDeleteOption

        _ui.value = _ui.value.copy(isLoading = true, error = null)
        try {
            val token = "Bearer ${authRepository.getAccessToken()}"

            // 삭제된 ID들을 누적해서 담을 변수
            val finalDeletedSectionIds = mutableSetOf<Long>()
            // 카드 ID를 두 소스에서 따로 받음
            val finalCardRestores = mutableListOf<CardRestoreInfo>() // (섹션 삭제 시)
            val finalDeletedCardIdsFromCardJob = mutableSetOf<Long>() // (카드 삭제 시)

            coroutineScope {
                // 섹션 삭제 API 호출
                val boardJob = if (selectedSectionIds.isNotEmpty()) {
                    async {
                        val request = BoardDeleteRequest(
                            boardIds = selectedSectionIds.map { it.toLong() },
                            deleteLinkedCards = boardDeleteOption
                        )
                        val response = boardService.deleteBoards(token, request)
                        if (response.isSuccessful && response.body() != null) {
                            response.body()!!.forEach {
                                finalDeletedSectionIds.add(it.boardId)
                                // CardRestoreInfo 리스트를 저장
                                finalCardRestores.addAll(it.cardRestores)
                            }
                            true // 성공
                        } else false // 실패
                    }
                } else null

                // 카드 삭제 API 호출
                val cardJob = if (selectedCardIds.isNotEmpty()) {
                    async {
                        val request = CardDeleteRequest(
                            cardIds = selectedCardIds.map { it.toLong() },
                            deleteMode = cardDeleteOption
                        )
                        val response = cardService.deleteCards(token, request)
                        if (response.isSuccessful && response.body() != null) {
                            response.body()!!.results.forEach {
                                // '카드만 삭제' ID를 별도 변수에 저장
                                finalDeletedCardIdsFromCardJob.add(it.cardId.toLong())
                            }
                            true // 성공
                        } else false // 실패
                    }
                } else null

                val boardResult = boardJob?.await() ?: true
                val cardResult = cardJob?.await() ?: true

                if (boardResult && cardResult) {
                    // 모든 삭제 작업 성공 시, 최종 ID들을 상태 변수에 저장
                    lastDeletedSectionIds = finalDeletedSectionIds
                    // 두 변수에 나누어 저장
                    lastDeletedCardRestores = finalCardRestores
                    lastDeletedCardIds = finalDeletedCardIdsFromCardJob

                    val deletedCount = selectedSectionIds.size + selectedCardIds.size
                    _deleteCompleteEvent.emit(deletedCount)
                    if (currentBoardId > -1) loadPage(reset = true)
                } else {
                    throw Exception("일부 항목 삭제에 실패했습니다.")
                }
            }
        } catch (e: Exception) {
            Log.e("BoardDetailVM", "Delete action failed", e)
            _toastMessage.value = "삭제에 실패했습니다."
            _ui.value = _ui.value.copy(isLoading = false, error = e.message ?: "삭제 중 오류가 발생했습니다.")
            // 실패 시 임시 ID 초기화
            lastDeletedSectionIds = emptySet()
            lastDeletedCardIds = emptySet()
            lastDeletedCardRestores = emptyList()
            lastCardDeleteMode = ""
        }
    }

    // --- 삭제 실행 취소(복구) 함수 (API 통합) ---
    suspend fun undoLastDeletion() {
        // 새 변수까지 함께 체크
        if (lastDeletedSectionIds.isEmpty() && lastDeletedCardIds.isEmpty() && lastDeletedCardRestores.isEmpty()) return

        _ui.value = _ui.value.copy(isLoading = true, error = null)
        try {
            val token = "Bearer ${authRepository.getAccessToken()}"
            // cardRestores 리스트를 조합
            val allCardRestores = mutableListOf<CardRestoreInfo>()

            // 2-1. 섹션과 함께 삭제된 카드들 (List<CardRestoreInfo>)
            allCardRestores.addAll(lastDeletedCardRestores)

            // 2-2. 카드만 삭제된 것들 (lastDeletedCardIds)
            if (lastDeletedCardIds.isNotEmpty()) {
                // 이 카드들은 현재 보드(currentBoardId) 소속으로 간주
                allCardRestores.add(
                    CardRestoreInfo(
                        cardIds = lastDeletedCardIds.toList(),
                        boardId = currentBoardId.toLong()
                        // (참고: lastCardDeleteMode는 '카드 복구' API용)
                    )
                )
            }

            // 복구 요청 DTO 생성
            val request = BoardRestoreRequest(
                boardIds = lastDeletedSectionIds.toList(), // 섹션 ID는 boardIds 필드에 담김
                sectionIds = emptyList(), // 상세화면에서는 섹션만 삭제하므로 sectionIds는 비워둠
                cardRestores = allCardRestores // 조합된 리스트 전달
            )

            // 통합된 복구 API 호출
            val response = boardService.restoreBoards(token, request)

            if (response.isSuccessful && response.body() != null) {
                val restoredBoards = response.body()!!.restoredBoardIds.size
                val restoredCards = response.body()!!.restoredCardIds.size

                val message = when {
                    restoredBoards > 0 && restoredCards > 0 -> "${restoredBoards}개 섹션과 ${restoredCards}개 카드 삭제가 취소되었습니다."
                    restoredBoards > 0 -> "${restoredBoards}개 섹션 삭제가 취소되었습니다."
                    restoredCards > 0 -> "${restoredCards}개 카드 삭제가 취소되었습니다."
                    else -> null
                }
                message?.let { _toastMessage.value = it }

                if (currentBoardId > -1) {
                    loadPage(reset = true)
                }
            } else {
                _toastMessage.value = "복구에 실패했습니다."
                throw Exception("Restore request failed with code ${response.code()}")
            }

        } catch (e: Exception) {
            Log.e("BoardDetailVM", "Undo deletion failed", e)
            _ui.value = _ui.value.copy(isLoading = false, error = e.message ?: "복구 중 오류가 발생했습니다.")
        } finally {
            // 성공/실패 여부와 관계없이 임시 ID는 모두 비움
            lastDeletedSectionIds = emptySet()
            lastDeletedCardIds = emptySet()
            lastDeletedCardRestores = emptyList()
            lastCardDeleteMode = ""
            _ui.value = _ui.value.copy(isLoading = false)
        }
    }
}
