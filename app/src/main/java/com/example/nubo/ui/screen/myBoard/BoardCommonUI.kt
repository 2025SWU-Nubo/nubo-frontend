package com.example.nubo.ui.screen.myBoard


import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
// 프로젝트 리소스 / theme
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles.b1_semibold_18
import com.example.nubo.ui.theme.AppTextStyles.b2_medium_16

// 커스텀 확장 함수 (너희 프로젝트 공용 noRippleClickable)
import com.example.nubo.ui.component.noRippleClickable
import com.example.nubo.ui.theme.Grey10
import com.example.nubo.ui.theme.Grey30
import com.example.nubo.ui.theme.Grey700


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
    BOARD_MEMBERS, // 참여자 목록 확인
    BOARD_RENAME // 보드 이름 변경
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

    // --- 동적 분기를 위해 추가 ---
    isSectionScreen: Boolean,  // 현재 화면이 섹션 상세인지 여부
    source: String?,           // "AI" / "USER" / null
    isShared: Boolean,         // 공유 보드인지
    isOwner: Boolean,          // 내가 owner인지

    // 콜백들
    onRenameClick: () -> Unit,
    onMembersClick: () -> Unit,
    onAddSectionClick: () -> Unit,
    onSelectCardClick: () -> Unit,
    onSelectSectionClick: () -> Unit,
    onDismiss: () -> Unit
) {
    // -----------------------
    // 1) 조건 기반 메뉴 생성
    // -----------------------
    val menuList = remember(isSectionScreen, source, isShared, isOwner) {
        when {
            // 1. 섹션 상세 화면
            isSectionScreen -> listOf(
                MenuRow("이름 변경", R.drawable.ic_board_rename, onRenameClick),
                MenuRow("항목 선택", R.drawable.ic_board_selectfile, onSelectCardClick)
            )

            // 2. 보드 source = AI
            source == "AI" -> listOf(
                MenuRow("섹션 추가", R.drawable.ic_board_addsection, onAddSectionClick),
                MenuRow("항목 선택", R.drawable.ic_board_selectfile, onSelectCardClick)
            )

            // 3. USER 보드 (단독 생성 보드)
            source == "USER" && !isShared -> listOf(
                MenuRow("이름 변경", R.drawable.ic_board_rename, onRenameClick),
                MenuRow("참여자 목록", R.drawable.ic_board_memerset, onMembersClick),
                MenuRow("섹션 추가", R.drawable.ic_board_addsection, onAddSectionClick),
                MenuRow("항목 선택", R.drawable.ic_board_selectfile, onSelectCardClick)
            )

            // 4. 공유보드 + owner
            isShared && isOwner -> listOf(
                MenuRow("이름 변경", R.drawable.ic_board_rename, onRenameClick),
                MenuRow("참여자 목록", R.drawable.ic_board_memerset, onMembersClick),
                MenuRow("섹션 추가", R.drawable.ic_board_addsection, onAddSectionClick),
                MenuRow("섹션 선택", R.drawable.ic_board_foldercheck_light, onSelectSectionClick),
                MenuRow("카드 선택", R.drawable.ic_board_selectfile, onSelectCardClick)
            )

            // 5. 공유보드 + owner 아님
            isShared && !isOwner -> listOf(
                MenuRow("참여자 목록", R.drawable.ic_board_memerset, onMembersClick),
                MenuRow("섹션 추가", R.drawable.ic_board_addsection, onAddSectionClick),
                MenuRow("섹션 선택", R.drawable.ic_board_foldercheck_light, onSelectSectionClick),
                MenuRow("카드 선택", R.drawable.ic_board_selectfile, onSelectCardClick)
            )

            else -> emptyList()
        }
    }

    // -----------------------
    // 2) 실제 UI (현재 UI 유지)
    // -----------------------
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
                .padding(top = 24.dp, bottom = 32.dp)
        ) {

            // --- 헤더 ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = b1_semibold_18,
                    color = Color(0xFF1A1A1A)
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "닫기",
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(24.dp)
                        .noRippleClickable { onDismiss() }
                )
            }

            Spacer(Modifier.height(28.dp))

            // --- 목록 ---
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                menuList.forEachIndexed { index, item ->
                    MenuRowItem(
                        icon = item.icon,
                        text = item.label,
                        showDivider = index != menuList.lastIndex,
                        onClick = item.onClick
                    )
                }
            }
        }
    }
}

private data class MenuRow(
    val label: String,
    val icon: Int,
    val onClick: () -> Unit
)

// 공용 Row 목록 아이템
@Composable
private fun MenuRowItem(
    icon: Int,
    text: String,
    showDivider: Boolean = true,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {

        // 클릭 가능한 항목 Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
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
                color = Grey700
            )
        }
        // Divider 아래 라인
        if (showDivider) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Grey10)
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
