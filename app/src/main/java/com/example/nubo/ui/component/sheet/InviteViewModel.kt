package com.example.nubo.ui.component.sheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update



class InviteViewModel(
    private val repo: InviteRepository = MockInviteRepository()
) : ViewModel(){

    // Search query from UI
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    // Invited (or selected) user ids/emails
    private val _selected = MutableStateFlow<Set<String>>(emptySet())
    val selected: StateFlow<Set<String>> = _selected.asStateFlow()

    // UI state (loading, results, error)
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<InviteUiState> =
        _query
            .debounce(300)              // debouncing input
            .map { it.trim() }
            .distinctUntilChanged()
            .flatMapLatest { keyword ->
                if (keyword.isBlank()) {
                    flowOf<InviteUiState>(InviteUiState.Idle)
                } else {
                    flow {
                        emit(InviteUiState.Loading)
                        try {
                            val res = repo.searchByEmail(keyword)
                            emit(InviteUiState.Success(res))
                        } catch (t: Throwable) {
                            emit(InviteUiState.Error(t.message ?: "Unknown error"))
                        }
                    }
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                InviteUiState.Idle
            )

    fun onQueryChange(newValue: String) {
        _query.value = newValue
    }

    fun toggleSelect(email: String) {
        _selected.update { set ->
            if (email in set) set - email else set + email
        }
    }

    fun clearAll() {
        _selected.value = emptySet()
    }

    fun inviteSelected(onSuccess: (Set<String>) -> Unit) {
        // TODO: call server invite API
        onSuccess(_selected.value)
    }
}
