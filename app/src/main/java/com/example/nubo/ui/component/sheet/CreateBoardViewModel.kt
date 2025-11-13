package com.example.nubo.ui.component.sheet

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nubo.data.repository.AuthRepository
import com.example.nubo.data.repository.BoardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateBoardViewModel @Inject constructor(
    private val boardRepository: BoardRepository,
    private val authRepository: AuthRepository
) : ViewModel(){

    private val _ui = MutableStateFlow(CreateBoardUiState())
    val ui: StateFlow<CreateBoardUiState>  = _ui.asStateFlow()

    fun resetForNewBoard() {
        // Reset all UI state to initial
        _ui.value = CreateBoardUiState()
    }

    fun onNameChange(text:String){
        _ui.update {it.copy(name = text, nameError = null)}
    }

    fun onSharedChange(shared: Boolean){
        _ui.update { it.copy(isShared = shared) }
    }

    fun setInvitedEmails(emails: List<String>) {
        _ui.update { it.copy(invitedEmails = emails) }
    }

    fun submit(){
        val name = _ui.value.name.trim()
        if(name.isEmpty()){
            _ui.update { it.copy(nameError = "보드 이름을 입력해주세요.") }
            return
        }

        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, nameError = null) }

            val token = "Bearer ${authRepository.getAccessToken()}"

            // 보드 이름 중복 확인
            val available = boardRepository
                .isBoardNameAvailable(token = token ,name = name)
                .getOrElse { false }
            if(!available){
                _ui.update {
                    it.copy(isLoading = false, nameError = "이미 존재하는 보드 이름이에요. 다른 이름을 입력해주세요.")
                }
                return@launch
            }


            // 보드 생성
            val emails = _ui.value.invitedEmails.takeIf { _ui.value.isShared && it.isNotEmpty() }
            val createResult = boardRepository.createBoard(
                token = token,
                name = name,
                shared = _ui.value.isShared,
                memberEmails = emails
            )

            createResult
                .onSuccess { item ->
                    _ui.update { it.copy(isLoading = false, created = item) }
                }
                .onFailure {
                    _ui.update { it.copy(
                        isLoading = false,
                        nameError = "요청 처리 중 오류가 발생했어요. 잠시 후 다시 시도해주세요."
                    ) }
                }
        }
    }

    fun submitWith(currentName: String) {
        val name = currentName.trim()
        if (name.isEmpty()) {
            _ui.update { it.copy(nameError = "보드 이름을 입력해주세요.") }
            return
        }

        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, nameError = null) }

            val token = "Bearer ${authRepository.getAccessToken()}"

            val available = boardRepository
                .isBoardNameAvailable(token = token, name = name)
                .getOrElse { false }
            if (!available) {
                _ui.update {
                    it.copy(isLoading = false, nameError = "이미 존재하는 보드 이름이에요. 다른 이름을 입력해주세요.")
                }
                return@launch
            }

            val emails = _ui.value.invitedEmails.takeIf { _ui.value.isShared && it.isNotEmpty() }
            val createResult = boardRepository.createBoard(
                token = token,
                name = name,
                shared = _ui.value.isShared,
                memberEmails = emails
            )

            createResult
                .onSuccess { item ->
                    _ui.update { it.copy(isLoading = false, created = item) }
                }
                .onFailure {
                    _ui.update {
                        it.copy(isLoading = false, nameError = "요청 처리 중 오류가 발생했어요. 잠시 후 다시 시도해주세요.")
                    }
                }
        }
    }



    fun consumeCreated() {
        _ui.update { it.copy(
            created = null,
            name = "",
            isShared = false,
            invitedEmails = emptyList()
        ) }
    }


}
