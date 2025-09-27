package com.example.nubo.ui.screen.editCard.widgets

import androidx.compose.* // 정리
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.example.nubo.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.example.nubo.R
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey5
import com.example.nubo.ui.theme.Grey50
import com.example.nubo.ui.theme.GreyMain100
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.PurpleMain500
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import com.example.nubo.ui.theme.Grey30
import com.example.nubo.ui.theme.Grey700
import com.example.nubo.ui.theme.Purple100

// 라벨+아이콘 프리셋 데이터
data class PresetAction(val label: String, @androidx.annotation.DrawableRes val iconRes: Int)

@Composable
fun AiPromptBar(
    value: String,
    loading: Boolean,
    onValueChange: (String) -> Unit,
    onClose: () -> Unit,
    onSubmit: () -> Unit,
    showAiBar: Boolean,
    canUndo: Boolean,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var tfv by remember { mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length))) }
    LaunchedEffect(value) { if (value != tfv.text) tfv = tfv.copy(text = value, selection = TextRange(value.length)) }
    LaunchedEffect(showAiBar) { if (showAiBar) focusRequester.requestFocus() }

    var selectedPreset by remember { mutableStateOf<Int?>(null) }
    val presets = remember {
        listOf(
            PresetAction("더 간결하게", R.drawable.arrows_inside),
            PresetAction("더 자세하게", R.drawable.arrows_outward),
            PresetAction("핵심만 하이라이트", R.drawable.format_ink_highlighter)
        )
    }
    val canSend by remember(loading, value) { mutableStateOf(!loading && value.isNotBlank()) }

    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 16.dp,
        color = Grey5,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        modifier = modifier
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // 프리셋 칩
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                presets.forEachIndexed { index, preset ->
                    val selected = selectedPreset == index
                    AssistChip(
                        onClick = {
                            val next = "${preset.label} "
                            tfv = TextFieldValue(next, TextRange(next.length))
                            onValueChange(next)
                            selectedPreset = index
                            focusRequester.requestFocus()
                        },
                        label = {
                            Text(
                                text = preset.label,
                                style = if (selected) AppTextStyles.label_SemiBold_12 else AppTextStyles.label_medium_12
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(preset.iconRes),
                                contentDescription = "${preset.label} 프리셋",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selected) Purple100 else Grey30,
                            labelColor = if (selected) PurpleMain500 else Color.Black,
                            leadingIconContentColor = if (selected) PurpleMain500 else Color.Unspecified,
                        ),
                        border = if (selected) BorderStroke(1.dp, PurpleMain500) else null,
                        shape = RoundedCornerShape(45.dp)
                    )
                }

                IconOnlyChip(
                    enabled = canUndo,
                    onClick = {
                        onUndo()
                        selectedPreset = null
                        focusRequester.requestFocus()
                    },
                    containerColor = if (canUndo) Purple100 else Grey30,
                    contentColor = if (canUndo) PurpleMain500 else GreyMain300,
                    borderColor = if (canUndo) PurpleMain500 else null
                ) {
                    Icon(painter = painterResource(R.drawable.replay), contentDescription = "되돌리기", tint = Color.Unspecified, modifier = Modifier.size(18.dp))
                }
            }

            // 입력 + 전송
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(painter = painterResource(R.drawable.ai_prompt_logo), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)) {
                    TextField(
                        value = tfv,
                        onValueChange = { newValue ->
                            if (loading) {
                                tfv = tfv.copy(selection = newValue.selection)
                                return@TextField
                            }
                            tfv = newValue
                            onValueChange(newValue.text)
                            selectedPreset = null
                        },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        placeholder = { Text(if (loading) "AI가 편집 중입니다..." else "더 간결하게 요약해줘.") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (canSend) { onSubmit(); focusRequester.requestFocus() }
                        })
                    )
                    if (loading) {
                        Box(
                            modifier = Modifier.matchParentSize().background(Color.White).padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text("Al가 편집 중이에요...", style = AppTextStyles.label_semibold_14, color = PurpleMain500)
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                val sendContentColor = if (canSend) PurpleMain500 else GreyMain100
                FilledIconButton(
                    onClick = { if (canSend) { onSubmit(); focusRequester.requestFocus() } },
                    enabled = true,
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Transparent, contentColor = sendContentColor)
                ) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = sendContentColor)
                    } else {
                        Icon(painterResource(R.drawable.ai_prompt_send), contentDescription = "전송", tint = sendContentColor)
                    }
                }
            }
        }
    }
}

