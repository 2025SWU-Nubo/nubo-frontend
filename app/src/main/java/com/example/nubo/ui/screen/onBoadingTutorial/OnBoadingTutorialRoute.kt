package com.example.nubo.ui.screen.onBoadingTutorial

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * 온보딩 튜토리얼 Route
 * - NavHost에서는 이 컴포저블만 사용
 * - 내부에서 ViewModel을 붙이고, 완료 시 서버에 "튜토리얼 완료"를 저장한 뒤
 *   관심사 or 홈으로 라우팅을 위임
 */
@Composable
fun OnBoardingTutorialRoute(
    needsInterest: Boolean,
    onGoInterest: () -> Unit,
    onGoHome: () -> Unit,
    vm: OnBoardingTutorialViewModel = hiltViewModel()
){
    TutorialScreen(
        onFinish = {
            // 1) 서버에 튜토리얼 완료 찍기
            vm.onTutorialFinished()

            // 2) 다음 화면으로 분기
            if (needsInterest) {
                onGoInterest()
            } else {
                onGoHome()
            }
        }
    )

}
