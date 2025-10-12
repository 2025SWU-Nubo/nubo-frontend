package com.example.nubo.ui.screen.card

import androidx.compose.foundation.layout.*
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
import com.example.nubo.ui.theme.AppTextStyles

@Composable
fun CardDetailRoute(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onInfoClick: (() -> Unit)? = null,
    viewModel: CardDetailViewModel = hiltViewModel(),
    navController: NavController
) {
    // Collect lifecycle-aware state from ViewModel
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val toast = viewModel.toast.collectAsStateWithLifecycle().value
    //레벨업, 누베리 획득 토스트
    val toast2 by viewModel.toast2.collectAsStateWithLifecycle()


    // 뒤로가기 시 LearnScreen에 데이터를 전달하는 로직
    val handleOnBack = {
        if (state is CardDetailUiState.Success) {
            val item = state.item
            val handle = navController.previousBackStackEntry?.savedStateHandle

            if (item.stageUp) {
                handle?.set("show_levelup_stage", item.stage)
            }
            if (item.berryGained) {
                handle?.set("show_berry_gained", true)
            }
        }
        onBack() // 원래의 뒤로가기 동작 실행
    }

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
                onEdit = onEdit,
                onBack = handleOnBack,
                onInfoClick = onInfoClick,
                onToggleFavorite = { viewModel.toggleFavorite() },
                toastMessage = toast,                  // 라우트에서 내려줌
                onConsumeToast = { viewModel.consumeToast() }, // 소모 콜백
                // 레벨업 토스트와 consume 함수 전달
                toastMessage2 = toast2,
                onConsumeToast2 = { viewModel.consumeToast2() }

            )
        }
    }
}

