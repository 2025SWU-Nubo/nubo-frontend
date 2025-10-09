package com.example.nubo.ui.screen.myBoard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.model.BoardRenameRequest
import com.example.nubo.data.model.BoardResponse
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

data class BoardDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val board: BoardResponse? = null,
    val favoriteOnly: Boolean = false,
    val page: Int = 0,
    val isLast: Boolean = false,
    val sort: String = "LATEST"
)

@HiltViewModel
class BoardDetailViewModel @Inject constructor(
    private val boardRepository: BoardRepository,
    private val authRepository: AuthRepository,
    private val boardService: BoardService
) : ViewModel() {
    private val _ui = MutableStateFlow(BoardDetailUiState())
    val ui: StateFlow<BoardDetailUiState> = _ui

    private var currentBoardId: Int = -1
    private var bootstrapped = false
    private val pageSize = 20

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
}

