package com.example.nubo.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.res.painterResource
import com.example.nubo.R
import com.example.nubo.model.myBoard.BoardItem
import com.example.nubo.ui.theme.AppTextStyles.b2_semibold_16
import com.example.nubo.ui.theme.AppTextStyles.label_medium_12
import com.example.nubo.ui.theme.DefaultText
import com.example.nubo.ui.theme.Grey200
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.GreyMain300
import kotlin.collections.chunked

@Composable
fun BoardContent(
    boards: List<BoardItem>,
    onCardClick: (BoardItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        items(boards.chunked(2)) { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { item ->
                    if (item.source == "AI") {
                        BoardCardWithText(
                            board = item,
                            onClick = { onCardClick(item) }
                        )
                    } else {
                        FullBoardCard(
                            board = item,
                            onClick = { onCardClick(item) }
                        )
                    }
                }
                if (rowItems.size < 2) {
                    Spacer(modifier = Modifier.width(190.dp))
                }
            }
        }
    }
}

    @Composable
fun BoardCardWithText(
        board: BoardItem,
        onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(190.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(top=4.dp, start = 4.dp, end = 4.dp, bottom = 11.dp)
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
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = GreyMain300,
                    modifier = Modifier.align(Alignment.Center)
                )
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
                    Icon(
                        painter = painterResource(
                            id = if (board.isBookmarked)
                                R.drawable.ic_board_star
                            else
                                R.drawable.ic_board_star
                        ),
                        contentDescription = "즐겨찾기 아이콘",
                        modifier = Modifier.size(16.dp)
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

@Composable
fun FullBoardCard(
    board: BoardItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(190.dp)
            .shadow(1.5.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(top=4.dp, start = 4.dp, end = 4.dp, bottom = 11.dp)
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
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = GreyMain300,
                    modifier = Modifier.align(Alignment.Center)
                )
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
                    Icon(
                        painter = painterResource(
                            id = if (board.isBookmarked)
                                R.drawable.ic_board_star
                            else
                                R.drawable.ic_board_star
                        ),
                        contentDescription = "즐겨찾기 아이콘",
                        modifier = Modifier.size(16.dp)
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
