package com.example.nubo.ui.screen.interest

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nubo.R

/**
 * 관심사 설정 화면 Route
 * - accessToken 을 외부에서 넘기지 않고, 내부(ViewModel/Repository)에서 조회하는 래퍼
 * - NavHost에서는 이 컴포저블만 사용하면 됨
 */
@Composable
fun OnBoardingInterestRoute(
    onBack: () -> Unit,
    onHome: () -> Unit,
    thumbnailsRes: Map<Long, Int> = emptyMap(), // 보드ID → 로컬 이미지 매핑(없으면 placeholder)
    vm: OnBoardingInterestViewModel = hiltViewModel()
) {
    // ViewModel이 보유한 AccessToken 노출 함수/Flow를 가정
    // 이미 구현돼 있다면 그걸 쓰고, 없으면 간단히 Repository에서 getAccessToken() 사용하도록 ViewModel에 함수 추가
    val tokenState by vm.accessToken.collectAsStateWithLifecycle(initialValue = null)

    // 토큰을 아직 못 얻었으면 그리기 지연
    val token = tokenState ?: return

    // 기존에 작성해둔 실제 화면 사용 (accessToken 파라미터만 주입)
    OnBoardingInterestScreen(
        accessToken = token,
        onBack = onBack,
        onHome = onHome,
        thumbnailsRes = thumbnailsRes,
        viewModel = vm
    )
}
