package com.example.nubo.ui.screen.card

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nubo.ui.theme.AppTextStyles

@Composable
fun CardDetailRoute(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onInfoClick: (() -> Unit)? = null,
    viewModel: CardDetailViewModel = hiltViewModel()
) {
    // Collect lifecycle-aware state from ViewModel
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val toast = viewModel.toast.collectAsStateWithLifecycle().value

    when (state) {
        is CardDetailUiState.Loading -> {
            // Simple full-screen loading
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is CardDetailUiState.Error -> {
            // Simple error view with back action
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.message,
                        style = AppTextStyles.b1_semibold_18
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onBack) {
                        Text(text = "뒤로가기")
                    }
                }
            }
        }

        is CardDetailUiState.Success -> {
            // Render actual detail screen
            CardDetailScreen(
                item = state.item,
                onBack = onBack,
                onEdit = onEdit,
                onInfoClick = onInfoClick,
                onToggleFavorite = { viewModel.toggleFavorite() },
                toastMessage = toast,                  // 라우트에서 내려줌
                onConsumeToast = { viewModel.consumeToast() } // 소모 콜백

            )
        }
    }
}

