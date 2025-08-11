package com.example.nubo.ui.screen.add

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(padding: PaddingValues = PaddingValues()) {
    Text("AddScreen", style = MaterialTheme.typography.bodyLarge)

        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val coroutineScope = rememberCoroutineScope()
        var isSheetOpen by remember { mutableStateOf(true) } // 진입 시 바로 열기

        if (isSheetOpen) {
            ModalBottomSheet(
                sheetState = sheetState,
                onDismissRequest = { isSheetOpen = false },
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                // 여기에 영상 / 보드 선택 UI
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("추가 생성하기", style = MaterialTheme.typography.titleMedium)

                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { /* 영상 로직 */ }
                    ) { Text("영상") }

                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { /* 보드 로직 */ }
                    ) { Text("보드") }
                }
            }
        }

}
