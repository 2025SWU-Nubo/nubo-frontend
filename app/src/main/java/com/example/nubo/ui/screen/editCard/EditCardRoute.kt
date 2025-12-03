package com.example.nubo.ui.screen.editCard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

@Composable
fun EditCardRoute(
    navController: NavController,
    onBack: () -> Unit,
    onSaved: () -> Unit, // parent can refresh detail, etc.
    viewModel: EditCardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (state) {
        is EditCardUiState.Loading,
        is EditCardUiState.Saving -> {
            LoadingBox()
        }

        is EditCardUiState.Error -> {
            val msg = (state as EditCardUiState.Error).message
            ErrorBox(msg, onBack)
        }

        // Saved 상태는 별도 화면 전환에 사용하지 않으므로 비워둠
        is EditCardUiState.Saved -> {
            // no-op
        }

        is EditCardUiState.Ready -> {
            Box(modifier = Modifier.fillMaxSize()) {
                EditCardScreen(
                    onBack = onBack, // this is usually navController::popBackStack
                    onSavedWithToast = { message ->
                        // parent에게 "저장됨" 콜백
                        onSaved()

                        // previous destination에서 토스트를 보여주기 위한 상태 전달
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("toast_after_edit", message)

                        // edit 화면 닫기
                        navController.popBackStack()
                    },
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun LoadingBox() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorBox(msg: String, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(msg)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onBack) { Text("뒤로가기") }
        }
    }
}
