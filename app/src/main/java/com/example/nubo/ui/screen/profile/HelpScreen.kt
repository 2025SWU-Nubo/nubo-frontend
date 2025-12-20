package com.example.nubo.ui.screen.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nubo.R
import com.example.nubo.ui.component.noRippleClickable
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey10
import com.example.nubo.ui.theme.Grey50

data class HelpSection(
    val title: String,
    val body: String
)

@Composable
fun HelpRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sections = listOf(
        HelpSection(
            title = "카드는 어떻게 저장하나요?",
            body = "공유하기에서 Nubo를 선택하면 자동으로 카드가 생성돼요. 또한 앱 내 하단 콘텐츠 추가 버튼을 이용해 영상 링크를 넣어 추가하는 방법도 있어요."
        ),
        HelpSection(
            title = "보드와 섹션은 어떻게 다른건가요?",
            body = "섹션은 보드 내 2차 분류를 위한 기능이에요. \n보드는 AI가 자동 생성하는 보드와 사용자가 직접 생성하는 보드로 이루어져 있지만 섹션은 사용자만 직접 추가할 수 있어요."
        ),
        HelpSection(
            title = "공유보드에서 다른 사람의 카드를 수정할 수 있나요?",
            body = "아니요. 카드는 직접 추가한 사용자만 자신의 카드를 수정할 수 있어요."
        ),
        HelpSection(
            title = "공유보드로 바뀌면 되돌릴 수 있나요?",
            body = "아니요. 공유보드로 생성하거나 변경한 보드는 개인 보드로 되돌릴 수 없어요."
        ),
        HelpSection(
            title = "알림이 안 와요?",
            body = "휴대폰 설정에서 알림 권한이 켜져 있는지 확인해주세요. 추가로 앱 내 알림 설정도 함께 확인해주세요."
        )
    )

    HelpScreen(
        title = "도움말",
        sections = sections,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    title: String,
    sections: List<HelpSection>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val expandedSaver = listSaver<SnapshotStateList<Boolean>, Boolean>(
        save = { stateList -> stateList.toList() },
        restore = { restored -> restored.toMutableStateList() }
    )

    val expandedStates = rememberSaveable(
        sections.size,
        saver = expandedSaver
    ) {
        MutableList(sections.size) { false }.toMutableStateList()
    }

    val bottomInset = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = title,
                        style = AppTextStyles.subtitle_semibold_20,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        focusManager.clearFocus(force = true)
                        onBack()
                    }) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = "뒤로가기")
                    }
                },
                modifier = Modifier.drawBehind {
                    val y = size.height
                    drawLine(
                        color = Grey50,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end   = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 18.dp,
                    bottom = bottomInset + 18.dp
                )
            ) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "자주 묻는 질문",
                        style = AppTextStyles.b2_regular_16,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(start = 32.dp, bottom = 12.dp)
                    )
                }

                itemsIndexed(sections) { index, section ->
                    HelpToggleItem(
                        title = section.title,
                        body = section.body,
                        expanded = expandedStates[index],
                        onToggle = {
                            expandedStates[index] = !expandedStates[index]
                        }
                    )

                    if (index != sections.lastIndex) {
                        Divider(
                            modifier = Modifier.padding(start = 32.dp, end = 24.dp),
                            color = Grey10
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpToggleItem(
    title: String,
    body: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
//    토글 리스트 아이콘 회전 인터랙션
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(
            durationMillis = 240,
            easing = FastOutSlowInEasing
        ),
        label = "toggleIconRotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClickable(onClick = onToggle)
            .padding(start = 32.dp, end = 24.dp, top = 16.dp, bottom = 16.dp)
            .animateContentSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = title,
                style = AppTextStyles.b1_semibold_18,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).padding(end = 8.dp)

            )

            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_forward),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(rotation)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = tween(240, easing = FastOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(160, delayMillis = 40)
            ),
            exit = shrinkVertically(
                shrinkTowards = Alignment.Top,
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            ) + fadeOut(
                animationSpec = tween(160, delayMillis = 110)
            )
        ) {
            Column {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = body,
                    style = AppTextStyles.b2_regular_16,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HelpScreenPreview() {
    MaterialTheme {
        HelpScreen(
            title = "도움말",
            sections = listOf(
                HelpSection(
                    title = "카드는 어떻게 저장하나요?",
                    body = "공유하기로 링크를 보내면 자동으로 카드가 생성돼요\n저장 후 보드에서 분류할 수 있어요"
                ),
                HelpSection(
                    title = "공유보드로 바뀌면 되돌릴 수 있나요?",
                    body = "아니요\n공유보드로 생성하거나 변경한 보드는 개인 보드로 되돌릴 수 없어요"
                )
            ),
            onBack = {}
        )
    }
}
