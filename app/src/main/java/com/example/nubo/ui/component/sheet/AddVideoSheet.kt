package com.example.nubo.ui.component.sheet

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey0
import com.example.nubo.ui.theme.Grey10
import com.example.nubo.ui.theme.Grey200
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.Grey500
import com.example.nubo.ui.theme.PurpleMain500

@Composable
fun AddVideoSheet(
    onClose: () -> Unit
) {
    // 페이지/입력/선택 상태 (네 코드 그대로)
    var page by rememberSaveable { mutableStateOf(SheetPage.SAVE_VIDEO) }
    var input by rememberSaveable { mutableStateOf("") }
    var checkedIds by rememberSaveable { mutableStateOf(setOf<String>()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ===== 공통 헤더 =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp, vertical = 12.dp)
        ) {
            /*IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(
                    painter = painterResource(id = R.drawable.close_icon),
                    contentDescription = "닫기",
                    tint = Grey500
                )*/
            /*}*/
            Text(
                text = when (page) {
                    SheetPage.SAVE_VIDEO -> "영상 저장하기"
                    SheetPage.PICK_BOARD -> "보드 선택"
                },
                style = AppTextStyles.b2_semibold_16,
                modifier = Modifier.align(Alignment.Center)
            )
            if (page == SheetPage.PICK_BOARD) {
                TextButton(
                    onClick = { /* TODO: 선택 완료 로직 */ onClose() },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) { Text("선택") }
            }
        }

        // ===== 페이지별 본문 =====
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp, vertical = 8.dp)
        ) {
            when (page) {
                SheetPage.SAVE_VIDEO -> {
                    // 입력창 + 버튼을 한 줄에 나란히 배치
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 15.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f),                          // 버튼 제외 나머지 폭
                            shape = RoundedCornerShape(10.dp),        // ✅ CreateBoardSheet와 동일
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurpleMain500,   // ✅ 동일
                                unfocusedBorderColor = Grey50,        // ✅ 동일
                                focusedContainerColor = Color.White,  // ✅ 동일
                                unfocusedContainerColor = Grey10      // ✅ 동일
                            ),
                            placeholder = {
                                // ✅ 예시 텍스트(문구/스타일) 그대로 유지
                                Text(
                                    "링크를 입력하세요",
                                    style = AppTextStyles.b2_medium_16,
                                    color = Grey200
                                )
                            },
                            textStyle = AppTextStyles.b2_medium_16
                        )

                        Button(
                            onClick = {
                                // TODO: 저장 로직
                                page = SheetPage.PICK_BOARD
                            },
                            modifier = Modifier.height(56.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text("추가", style = AppTextStyles.label_semibold_14, color = Grey0)
                        }
                    }

                    Spacer(Modifier.height(22.dp))
                }

                SheetPage.PICK_BOARD -> {
                    // 간단한 트리 + 체크박스
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 240.dp, max = 520.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(sampleTree(), key = { it.id }) { node ->
                            BoardNodeItem(
                                node = node,
                                level = 0,
                                checked = checkedIds.contains(node.id),
                                onCheckedChange = { id, isOn ->
                                    checkedIds = if (isOn) checkedIds + id else checkedIds - id
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class SheetPage { SAVE_VIDEO, PICK_BOARD }

// ------------------------- 보드 선택용 컴포넌트들 -------------------------

private data class BoardNode(
    val id: String,
    val title: String,
    val children: List<BoardNode> = emptyList(),
    val selectable: Boolean = true
)

@Composable
private fun BoardNodeItem(
    node: BoardNode,
    level: Int,
    checked: Boolean,
    onCheckedChange: (String, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (16 * level).dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (node.children.isNotEmpty()) {
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "접기" else "펼치기",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { expanded = !expanded }
                )
                Spacer(Modifier.width(4.dp))
            } else {
                Spacer(Modifier.width(28.dp)) // 아이콘 자리 맞춤
            }

            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(node.title, style = MaterialTheme.typography.bodyLarge)
                    if (node.selectable) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { onCheckedChange(node.id, it) }
                        )
                    }
                }
            }
        }

        if (expanded) {
            node.children.forEach {
                BoardNodeItem(
                    node = it,
                    level = level + 1,
                    checked = false,
                    onCheckedChange = onCheckedChange
                )
            }
        }
    }
}

private fun sampleTree() = listOf(
    BoardNode("g1", "썸네일", selectable = false, children = listOf(
        BoardNode("c1", "누끼"),
        BoardNode("g1-1", "섹션 이름", selectable = false, children = listOf(
            BoardNode("c2", "새 섹션")
        ))
    )),
    BoardNode("g2", "1차 보드 이름", selectable = false, children = listOf(
        BoardNode("c3", "섹션 이름")
    ))
)
