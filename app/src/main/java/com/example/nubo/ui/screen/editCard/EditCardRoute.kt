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

@Composable
fun EditCardRoute(
    onBack: () -> Unit,
    onSaved: ()-> Unit,
    viewModel: EditCardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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
            // Option A: 바로 뒤로가기
            LaunchedEffect(Unit) { onSaved() }
        }
        is EditCardUiState.Ready -> {
            // Scaffold 제거하고 EditCardScreen이 직접 Scaffold를 처리하도록
            Box(modifier = Modifier.fillMaxSize()) {
                EditCardScreen(
                    onBack = onBack,
                    onSave = { viewModel.save() },
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

