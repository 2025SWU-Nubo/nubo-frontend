@file:Suppress("UnusedMaterial3ScaffoldPaddingParameter")
package com.example.nubo.ui.screen.card

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * IME 상단 밀착 동작을 검증하기 위한 단일 스크린 예제
 *
 * 핵심
 * - Scaffold 시스템 인셋 0으로 두고 innerPadding은 본문에만 적용
 * - 하단 오버레이 컨테이너에만 WindowInsets.ime 적용
 * - 오버레이 컴포넌트 자체에는 imePadding 미적용  align(BottomCenter)만 사용
 * - AI 바가 열리면 FAB 숨김  에디터 포커스 시 마크다운 바 노출
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImeOverlayDemoScreen() {
    // 상태
    var showAiBar by remember { mutableStateOf(false) }      // AI 바 표시 여부
    var showMdBar by remember { mutableStateOf(false) }      // 마크다운 바 표시 여부
    var text by remember { mutableStateOf(TextFieldValue("")) } // 입력 텍스트
    val focusManager = LocalFocusManager.current

    Scaffold(
        // 시스템 인셋은 수동 제어  innerPadding만 사용
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("IME Overlay Demo") },
                actions = {
                    // 마크다운 바 강제 열기
                    IconButton(onClick = {
                        showMdBar = true
                        showAiBar = false
                    }) { Icon(Icons.Default.Edit, contentDescription = "Markdown Bar") }

                    // AI 바 토글
                    IconButton(onClick = {
                        showAiBar = !showAiBar
                        if (showAiBar) showMdBar = false
                    }) { Icon(Icons.Default.SmartToy, contentDescription = "AI Bar") }
                }
            )
        },
        floatingActionButton = {
            // AI 바 열리면 FAB 숨김
            AnimatedVisibility(visible = !showAiBar) {
                FloatingActionButton(onClick = {
                    showAiBar = true
                    showMdBar = false
                    focusManager.clearFocus()
                }) { Icon(Icons.Default.SmartToy, contentDescription = "Open AI Bar") }
            }
        }
    ) { innerPadding ->

        Box(Modifier.fillMaxSize()) {

            // ── 본문 레이어  innerPadding만 적용 ──
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding) // Scaffold에서 넘어온 여백은 본문에만
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("입력창에 포커스하면 키보드가 열림  하단 바가 키보드 상단에 딱 붙는지 확인")

                // 간단 입력 영역  포커스 시 마크다운 바 노출
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp)
                        .background(Color(0xFFF2F2F2))
                        .padding(12.dp)
                        .onFocusChanged { fs ->
                            if (fs.isFocused) {
                                showMdBar = true
                                showAiBar = false
                            }
                        }
                )

                Spacer(Modifier.height(600.dp)) // 스크롤 영역 확보
            }

            // ── 하단 오버레이 레이어 ──
            // 중요  ime inset은 이 컨테이너에만 적용  바들에는 적용하지 않음
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.ime) // 키보드 높이만큼 전체를 위로
            ) {
                // 마크다운 툴바  에디터 포커스 시만
                AnimatedVisibility(
                    visible = showMdBar && !showAiBar,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    MarkdownBarStub(
                        onClose = { showMdBar = false }
                    )
                }

                // AI 프롬프트 바  FAB 또는 액션으로 열림
                AnimatedVisibility(
                    visible = showAiBar,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    AiBarStub(
                        onClose = { showAiBar = false },
                        onSubmit = {
                            // 실제 요청 연결 지점
                            showAiBar = false
                        }
                    )
                }
            }
        }
    }
}

/** 마크다운 툴바 대체용 단순 컴포넌트  실제 프로젝트 컴포넌트로 교체 가능 */
@Composable
private fun MarkdownBarStub(onClose: () -> Unit) {
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Markdown Toolbar  키보드 상단 밀착 확인")
            TextButton(onClick = onClose) { Text("닫기") }
        }
    }
}

/** AI 프롬프트 바 대체용 단순 컴포넌트  실제 프로젝트 컴포넌트로 교체 가능 */
@Composable
private fun AiBarStub(onClose: () -> Unit, onSubmit: () -> Unit) {
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("AI Prompt Bar  키보드 상단 밀착 확인")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onSubmit) { Text("전송") }
                OutlinedButton(onClick = onClose) { Text("닫기") }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun ImeOverlayDemoScreenPreview() {
    MaterialTheme { ImeOverlayDemoScreen() }
}
