package com.example.nubo.ui.screen.editCard


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@Composable
fun EditCardRoute(
    navController: NavController,
    onBack: () -> Unit,
    onSaved: ()-> Unit,
    viewModel: EditCardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
//    val navController = rememberNavController()
//    val state = viewModel.uiState.collectAsStateWithLifecycle().value
//    val snackbarHostState = remember { SnackbarHostState() }

    when (state) {
        is EditCardUiState.Loading -> LoadingBox()
        is EditCardUiState.Error -> {
            val msg = (state as EditCardUiState.Error).message
            ErrorBox(msg, onBack)
        }
        is EditCardUiState.Saving -> LoadingBox()
        is EditCardUiState.Saved -> {
//            LaunchedEffect(Unit) { onSaved() }
        }
        is EditCardUiState.Ready -> {
            // Scaffold 제거하고 EditCardScreen이 직접 Scaffold를 처리하도록
            Box(modifier = Modifier.fillMaxSize()) {
                EditCardScreen(
                    onBack = onBack,
                    onSave = { viewModel.save() },
                    onSaveWithToast = { message ->
                        //이전 백스택 엔트리의 SavedStateHandle에 토스트 메시지 저장
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("toast_after_edit",message)
                        //뒤로가기 실행
                        navController.popBackStack()
                    },
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable private fun LoadingBox() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
@Composable private fun ErrorBox(msg: String, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(msg)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onBack) { Text("뒤로가기") }
        }
    }
}

