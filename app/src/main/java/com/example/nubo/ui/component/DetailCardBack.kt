package com.example.nubo.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.nubo.model.card.CardDetailItem
import com.example.nubo.R
import com.example.nubo.ui.theme.Grey30
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.material3.RichText

@Composable
fun DetailCardBack(
    item: CardDetailItem,
    onDismiss: () -> Unit,
    onFlip: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // 흰색 배경 (테두리 역할)
            .clip(RoundedCornerShape(16.dp))
            .padding(2.dp) // 8dp 테두리 두께
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(RoundedCornerShape(10.dp)) // 내부 카드에 둥근 모서리 (선택사항)
                .border(
                    width = 1.dp,
                    color = Grey30,
                    shape = RoundedCornerShape(10.dp)
                )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 닫기 버튼
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Transparent)
                            .border(width = 1.dp, color = Grey30, shape = RoundedCornerShape(12.dp))
                    ) {
                        IconButton(onClick = { onDismiss() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.close_icon),
                                contentDescription = "닫기",
                                tint = Color.Black
                            )
                        }
                    }

                    // Flip 버튼
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Transparent)
                            .border(width = 1.dp, color = Grey30, shape = RoundedCornerShape(12.dp))
                    ) {
                        IconButton(onClick = { onFlip() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.cached_icon),
                                contentDescription = "Flip",
                                tint = Color.Black
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(item.title, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    RichText {
                        Markdown(item.summary)
                    }
                }
            }
        }
    }
}
