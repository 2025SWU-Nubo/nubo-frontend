package com.example.nubo.ui.screen.card

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    navController: NavController,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onInfoClick: (() -> Unit)? = null,
    viewModel: CardDetailViewModel = hiltViewModel(),
) {
    // 카드 상세 화면의 고유 토스트(즐겨찾기 토스트)
    val toast = viewModel.toast.collectAsStateWithLifecycle().value
    //레벨업, 누베리 획득 토스트
    val toast2 by viewModel.toast2.collectAsStateWithLifecycle()



    //  편집 화면에서 넘어온 토스트 메시지를 SavedStateHandle로 구독
    val handle = navController.currentBackStackEntry?.savedStateHandle
    val toastFromEditState = handle
        ?.getStateFlow("toast_after_edit", null as String?)
        ?.collectAsStateWithLifecycle()

    val toastFromEdit: String? = toastFromEditState?.value
     // 두 소스를 합침. 요약 카드 수정에서 카드 상세 복귀 토스트가 있으면 우선 사용
    val mergedToast = toastFromEdit ?: toast

    // 편집 복귀 토스트일 때만 지연 적용
    val toastDelayMs: Int = if (toastFromEdit != null) 180 else 0


    // 상태 수집
    val state = viewModel.uiState.collectAsStateWithLifecycle().value



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
                onBack =onBack,
                onInfoClick = onInfoClick,
                onToggleFavorite = { viewModel.toggleFavorite() },
                toastMessage = mergedToast,               // 합쳐진 토스트와 소비 콜백 전달
                onConsumeToast = {
                    if (toastFromEdit != null) {
                        // 카드 수정 페이지에서 온 토스트이면 saveStateHandle 값을 null로 소거
                        handle?.set("toast_after_edit", null)
                    } else {
                        // 그 외(ViewModel 토스트)는 기존 로직으로 소거
                        viewModel.consumeToast()
                    }
                },
                toastDelayMillis = toastDelayMs,
                // 레벨업 토스트와 consume 함수 전달
                toastMessage2 = toast2,
                onConsumeToast2 = { viewModel.consumeToast2() }
            )
        }
    }
}

