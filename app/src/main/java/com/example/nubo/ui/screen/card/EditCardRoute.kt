package com.example.nubo.ui.screen.card


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EditCardRoute(
    onBack: () -> Unit,
    viewModel: EditCardViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val snackbarHostState = remember { SnackbarHostState() }

    when (state) {
        is EditCardUiState.Loading -> LoadingBox()
        is EditCardUiState.Error -> ErrorBox(state.message, onBack)
        is EditCardUiState.Saving -> LoadingBox()
        is EditCardUiState.Saved -> {
            // Option A: 바로 뒤로가기
            LaunchedEffect(Unit) { onBack() }
        }
        is EditCardUiState.Ready -> {
            Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
                EditCardScreen(
                    summary = state.summary,
                    highlights = state.highlights,
                    onBack = onBack,
                    onSummaryChange = viewModel::updateSummary,
                    onToggleHighlight = viewModel::toggleHighlight,
                    onSave = { viewModel.save(onSuccess = {
                        // Option B: 남아서 스낵바 노출 후 뒤로가기
                        // LaunchedEffect(Unit) {
                        //   snackbarHostState.showSnackbar("저장되었습니다")
                        //   onBack()
                        // }
                    }) }
                )
            }
        }
    }
}

@Composable private fun LoadingBox() {
    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        CircularProgressIndicator()
    }
}
@Composable private fun ErrorBox(msg: String, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Text(msg)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onBack) { Text("뒤로가기") }
        }
    }
}

