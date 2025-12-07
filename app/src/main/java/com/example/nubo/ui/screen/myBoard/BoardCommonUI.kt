package com.example.nubo.ui.screen.myBoard


import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
// UI 이벤트
import androidx.compose.foundation.clickable
// 프로젝트 리소스 / theme
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles.b1_semibold_18
import com.example.nubo.ui.theme.AppTextStyles.b2_medium_16

// 커스텀 확장 함수 (너희 프로젝트 공용 noRippleClickable)
import com.example.nubo.ui.component.noRippleClickable


/** 보드 상세, 섹션 상세 공통 메뉴 및 enum 파일 */

// --- 공통 ENUM 정의 ---

// 어떤 바텀 시트가 보이는지 관리하기 위한 enum 추가
enum class BottomSheetType {
    NONE,           // 아무것도 안 보임
    MENU,           // 공통 메뉴
    SELECTION,      // 기존 카드 및 섹션 선택 모드 (삭제, 복제, 이동)
    BOARD_SELECTION, // 보드 선택 모드 (삭제)
    SECTION_RENAME,// 섹션 이름 변경
    SECTION_ADD, // 섹션 생성
    INVITE, // 초대 화면
    BOARD_MEMBERS // 참여자 목록 확인
}

// 복사 / 이동 공통 액션
enum class BoardAction {
    COPY,
    MOVE
}

// 공통 메뉴 콘텐츠
@Composable
fun MenuContent(
    title: String = "편집하기",
    onRenameClick: () -> Unit,
    onMembersClick: () -> Unit,
    onAddSectionClick: () -> Unit,
    onSelectCardClick: () -> Unit,
    onSelectSectionClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 13.dp, bottom = 20.dp)
        ) {

            // --- 헤더 영역 ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "닫기",
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(48.dp)
                        .noRippleClickable { onDismiss() }
                )

                Text(
                    text = title,
                    style = b1_semibold_18,
                    color = Color(0xFF1A1A1A)
                )
            }

            Spacer(Modifier.height(16.dp))

            // --- 목록 ---
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {

                MenuRowItem(
                    icon = R.drawable.ic_board_rename,
                    text = "이름 변경",
                    onClick = onRenameClick
                )

                MenuRowItem(
                    icon = R.drawable.ic_board_memerset,
                    text = "참여자 관리",
                    onClick = onMembersClick
                )

                MenuRowItem(
                    icon = R.drawable.ic_board_addsection,
                    text = "섹션 추가",
                    onClick = onAddSectionClick
                )

                MenuRowItem(
                    icon = R.drawable.ic_board_selectfile,
                    text = "카드 선택",
                    onClick = onSelectCardClick
                )

                MenuRowItem(
                    icon = R.drawable.ic_board_foldercheck_light,
                    text = "섹션 선택",
                    onClick = onSelectSectionClick
                )
            }
        }
    }
}


// ======================
// 공용 Row 아이템
// ======================
@Composable
private fun MenuRowItem(
    icon: Int,
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .noRippleClickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(20.dp)
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = text,
            style = b2_medium_16,
            color = Color(0xFF1A1A1A)
        )
    }
}
