package com.example.nubo.ui.screen.card


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EditCardRoute(
    onBack: () -> Unit,
    onSaved: ()-> Unit,
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
            LaunchedEffect(Unit) { onSaved() }
        }
        is EditCardUiState.Ready -> {
            // Scaffold 제거하고 EditCardScreen이 직접 Scaffold를 처리하도록
            Box(modifier = Modifier.fillMaxSize()) {
                EditCardScreen(
                    summary = state.summary,
                    highlights = state.highlights,
                    onBack = onBack,
                    onSummaryChange = viewModel::updateSummary,
                    onToggleHighlight = viewModel::toggleHighlight,
                    onSave = { viewModel.save(
                        onSuccess = {
                            // 상태 처리
                        }
                    )}
                )

                // SnackbarHost는 EditCardScreen 위에 오버레이로 표시
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(16.dp),
                    contentAlignment = androidx.compose.ui.Alignment.BottomCenter
                ) {
                    SnackbarHost(snackbarHostState)
                }
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

