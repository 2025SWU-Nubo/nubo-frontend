package com.example.nubo.data.repository

import com.example.nubo.data.model.InterestSubmitRequest
import com.example.nubo.data.network.BoardService
import com.example.nubo.data.network.UserService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 관심사(온보딩) 도메인의 데이터 접근 계층
 * - UI 단에서는 토큰만 넘겨주면, 실제 API 호출 형식은 레포지토리가 책임짐
 */
class InterestRepository @Inject constructor(
    private val userApi: UserService,
    private val boardApi: BoardService
) {
    /** 기본 보드 목록 조회 */
    suspend fun loadDefaultBoards() =
        boardApi.getDefaultBoards()

    /** 관심사 설정: 보드 선택 제출 */
    suspend fun submitSelectedBoards( ids: List<Long>) =
        userApi.submitInterests(
            body = InterestSubmitRequest(skip = false, selectedBoardIds = ids)
        )

    /** 관심사 설정: 건너뛰기 제출 */
    suspend fun submitSkip() =
        userApi.submitInterests(
            body = InterestSubmitRequest(skip = true, selectedBoardIds = null)
        )
}
