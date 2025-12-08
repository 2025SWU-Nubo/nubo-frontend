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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.shadow
import kotlinx.coroutines.launch
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles.b1_semibold_18
import com.example.nubo.ui.theme.AppTextStyles.b2_medium_16
import com.example.nubo.ui.component.noRippleClickable
import com.example.nubo.ui.theme.Grey10
import com.example.nubo.ui.theme.Grey700
import com.example.nubo.ui.theme.GreyMain300


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


// 모든 보드 내부 바텀시트에서 공통으로 사용하는 컨테이너
// ---------------------------------------------
// 기능:
// - 뒤 배경 Dim 처리
// - 배경 터치 시 닫기
// - 위 핸들(Handle) 표시
// - 아래로 드래그해서 닫기
// - 상단 라운드 코너 처리
// - 콘텐츠는 ColumnScope로 받아서 시트 내부에 배치
// ---------------------------------------------
@Composable
fun BottomSheetContainer(
    visible: Boolean,            // 시트가 열려 있는지 여부
    onDismiss: () -> Unit,       // 시트 닫기 콜백
    content: @Composable ColumnScope.() -> Unit // 시트 내부 UI
) {

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight }
        ),
        exit = slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight }
        )
    ) {
        // 드래그 이동값 (y축 오프셋)
        val offsetY = remember { Animatable(0f) }

        val scope = rememberCoroutineScope()

        Box(modifier = Modifier.fillMaxSize()) {
            // 실제 바텀시트 영역
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    // 드래그한 만큼 아래로 이동
                    .offset { IntOffset(0, offsetY.value.roundToInt()) }
                    // 바텀시트 그림자 적용
                    .shadow(
                        elevation = 22.dp,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        clip = false
                    )
                    // 드래그 처리
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            // snapTo는 suspend 함수 → launch 안에서 호출해야 함
                            scope.launch {
                                offsetY.snapTo(offsetY.value + delta)
                            }
                        },
                        onDragStopped = {
                            scope.launch {
                                if (offsetY.value > 200f) {
                                    onDismiss()
                                } else {
                                    offsetY.animateTo(0f, tween(200))
                                }
                            }
                        }
                    )
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color.White)
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {

                    // 상단 핸들(Handle)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 50.dp, height = 3.dp)
                                .clip(RoundedCornerShape(50))
                                .background(GreyMain300)
                        )
                    }
                    // 실제 콘텐츠
                    content()
                }
            }
        }
    }
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
    isMine :Boolean,           // 내가 생성자인지

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
    val menuList = remember(isSectionScreen, source, isShared, isOwner,isMine) {

        // 0. 공유보드 + 섹션 내부 + owner=false + mine=false → 카드 선택만 표시
        if (isSectionScreen && isShared && !isOwner && !isMine) {
            return@remember listOf(
                MenuRow("카드 선택", R.drawable.ic_board_selectfile, onSelectCardClick)
            )
        }

        when {
            // 1. 섹션 상세 화면
            isSectionScreen -> listOf(
                MenuRow("이름 변경", R.drawable.ic_board_rename, onRenameClick),
                MenuRow("카드 선택", R.drawable.ic_board_selectfile, onSelectCardClick)
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
