package com.example.nubo.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.ui.Alignment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.example.nubo.R
import com.example.nubo.model.myBoard.BoardItem
import com.example.nubo.ui.theme.AppTextStyles.b2_semibold_16
import com.example.nubo.ui.theme.AppTextStyles.label_medium_12
import com.example.nubo.ui.theme.DefaultText
import com.example.nubo.ui.theme.Grey10
import com.example.nubo.ui.theme.Grey20
import com.example.nubo.ui.theme.Grey200
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.Purple200
import com.example.nubo.ui.theme.Purple300
import com.example.nubo.ui.theme.Purple50
import kotlin.collections.chunked

@Composable
fun BoardContent(
    boards: List<BoardItem>,
    onBoardClick: (BoardItem) -> Unit,
    onBoardLongClick: (BoardItem) -> Unit,
    onFavoriteClick: (BoardItem) -> Unit, // 즐겨찾기 클릭 콜백
    //선택 관련 파라미터
    isSelectionMode: Boolean,
    selectedBoardIds: Set<Int>

) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp), // 좌우 16dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            boards.chunked(2).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // --- 첫 번째 아이템 ---
                    // Column을 사용하고 weight(1f)를 주어 공간을 50% 차지
                    Column(modifier = Modifier.weight(1f)) {
                        Box {
                            BoardCardWithText(
                                board = rowItems[0], // 첫 번째 아이템
                                onClick = { onBoardClick(rowItems[0]) },
                                onLongClick = { onBoardLongClick(rowItems[0]) },
                                onFavoriteClick = onFavoriteClick,
                                isSelectionMode = isSelectionMode,
                                // ID 비교
                                isSelected = selectedBoardIds.contains(rowItems[0].serverBoardId)
                            )
                            // AI 마크 아이콘을 밖에서 그림
                            if (rowItems[0].source == "AI") {
                                Icon(
                                    painter = painterResource(id = R.drawable.board_ai_mark),
                                    contentDescription = "AI 보드 마크",
                                    tint = Color.Unspecified,
                                    modifier = Modifier
                                        .align(Alignment.TopStart) // 정렬 기준
                                        .padding(start = 8.dp) // 좌측 여백
                                        .offset(y = (-1).dp)
                                        .size(32.dp)
                                )
                            }
                        }
                    }
                    // --- 두 번째 아이템 (또는 빈 공간) ---
                    if (rowItems.size > 1) {
                        // 두 번째 아이템이 있으면, 동일하게 weight(1f)를 가진 Column에 배치
                        Column(modifier = Modifier.weight(1f)) {
                            Box {
                                BoardCardWithText(
                                    board = rowItems[1], // 두 번째 아이템
                                    onClick = { onBoardClick(rowItems[1]) },
                                    onLongClick = { onBoardLongClick(rowItems[1]) },
                                    onFavoriteClick = onFavoriteClick,
                                    isSelectionMode = isSelectionMode,
                                    // ID 비교
                                    isSelected = selectedBoardIds.contains(rowItems[1].serverBoardId)
                                )
                                // AI 마크 아이콘을 밖에서 그림
                                if (rowItems[1].source == "AI") {
                                    Icon(
                                        painter = painterResource(id = R.drawable.board_ai_mark),
                                        contentDescription = "AI 보드 마크",
                                        tint = Color.Unspecified,
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(start = 8.dp)
                                            .offset(y = (-1).dp)
                                            .size(32.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // 아이템이 하나뿐이면, 오른쪽 절반을 빈 Spacer로 채움
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BoardCardWithText(
    board: BoardItem,
    onClick: () -> Unit,
    onFavoriteClick: (BoardItem) -> Unit,
    // 보드 상세 화면 및 섹션 상세 화면 선택 관련 파라미터
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onLongClick: () -> Unit // 길게 클릭
) {
    Box(
        modifier = Modifier
            .width(182.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .combinedClickable( // 일반 클릭과 롱클릭을 함께 처리
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(top = 4.dp, bottom = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .width(182.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(182.dp)
                    .height(120.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Grey50),
            ) {
                if (!board.imageUrl.isNullOrEmpty()) {
                    // 썸네일이 있을 때
                    AsyncImage(
                        model = board.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop // 가운데부터 꽉 차게
                    )
                } else {
                    // 썸네일이 비어있을 때
                    AsyncImage(
                        model = board.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop // 가운데부터 꽉 차게
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Purple50, RoundedCornerShape(6.dp))
                            .clip(RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.board_nubo_logo), // 아이콘을 nubo_logo로 변경
                            contentDescription = null,
                            tint = Purple300, //  연한 보라
                            modifier = Modifier.size(100.dp) // 로고 크기 조절
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 텍스트 영역은 카드 바깥에 표시됨
            Column(modifier = Modifier.padding(horizontal = 3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = board.title,
                        style = b2_semibold_16,
                        color = DefaultText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis, // 말줄임표 추가
                        modifier = Modifier.weight(1f, fill = false) // 남는 공간만 차지
                    )
                    // 즐겨찾기 아이콘 (빈별/채운별 리소스 교체)
                    Icon(
                        painter = painterResource(
                            id = if (board.isBookmarked)
                                R.drawable.ic_board_fillstar
                            else
                                R.drawable.ic_board_star
                        ),
                        contentDescription = "즐겨찾기 아이콘",
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onFavoriteClick(board) }, // 클릭 시 콜백 호출
                        tint = Color.Unspecified // 리소스 원본 색 유지
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        text = "${board.subtitle}",
                        style = label_medium_12,
                        color = DefaultText,
                        maxLines = 1
                    )
                    Text(
                        text = " • ${board.createdAt}",
                        style = label_medium_12,
                        color = Grey200,
                        maxLines = 1
                    )
                }
            }
        }
        // --- 선택 모드 오버레이 ---
        if (isSelectionMode && isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize() // 부모(기존 아이템)와 크기를 맞춤
                    .background(Color.White.copy(alpha = 0.5f))
                    .clip(RoundedCornerShape(12.dp))
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_board_selected), // 체크 아이콘
                contentDescription = "선택됨",
                tint = Color.Unspecified,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 10.dp, end = 8.dp) // 패딩값 조정
            )
        }
    }
}

@Composable
fun FullBoardCard(
    board: BoardItem,
    onClick: () -> Unit,
    onFavoriteClick: (BoardItem) -> Unit
) {
    Box(
        modifier = Modifier
            .width(190.dp)
            .shadow(1.5.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(top = 4.dp, start = 4.dp, end = 4.dp, bottom = 11.dp)
    ) {
        Column(
            modifier = Modifier
                .width(182.dp)
                .clickable { onClick() }
        ) {
            Box(
                modifier = Modifier
                    .width(182.dp)
                    .height(130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Grey50),
            ) {
                if (!board.imageUrl.isNullOrEmpty()) {
                    // 썸네일이 있을 때
                    AsyncImage(
                        model = board.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop // 가운데부터 꽉 차게
                    )
                } else {
                    // 기존 아이콘 표시
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = GreyMain300,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 텍스트 영역은 카드 바깥에 표시됨
            Column(modifier = Modifier.padding(horizontal = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = board.title,
                        style = b2_semibold_16,
                        color = DefaultText,
                        maxLines = 1
                    )
                    // 즐겨찾기 아이콘 (빈별/채운별 리소스 교체)
                    Icon(
                        painter = painterResource(
                            id = if (board.isBookmarked)
                                R.drawable.ic_board_fillstar
                            else
                                R.drawable.ic_board_star
                        ),
                        contentDescription = "즐겨찾기 아이콘",
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onFavoriteClick(board) }, // 클릭 시 콜백 호출
                        tint = Color.Unspecified // 리소스 원본 색 유지
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        text = "${board.subtitle}",
                        style = label_medium_12,
                        color = DefaultText,
                        maxLines = 1
                    )
                    Text(
                        text = " • ${board.createdAt}",
                        style = label_medium_12,
                        color = Grey200,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
