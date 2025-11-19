package com.example.nubo.ui.component.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.Grey10
import com.example.nubo.ui.theme.Grey30
import com.example.nubo.ui.theme.GreyMain100
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.PurpleMain500

/**
 * 재사용 가능한 2버튼 기본 다이얼로그
 *
 * - 용도: 제목 + 본문 1~3줄 + 버튼 2개(주/부) 패턴을 통일하기 위한 컴포넌트
 * - visible 이 false면 렌더링하지 않음
 * - body1~3은 필요한 줄만 채워서 전달
 * - 색상/라운드 등은 파라미터로 조절 가능
 */
@Composable
fun TwoButtonBasicDialog(
    // 표시 여부
    visible: Boolean,

    // 텍스트 영역
    title: String,
    body1: String? = null,
    body2: String? = null,
    body3: String? = null,

    // 버튼 라벨 (항상 2개)
    primaryButtonText: String,
    secondaryButtonText: String,

    // 콜백
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit,
    onDismiss: () -> Unit,

    // 스타일 옵션(필요 시 오버라이드)
    containerColor: Color = MaterialTheme.colorScheme.surface,
    primaryContainerColor: Color = PurpleMain500,
    primaryContentColor: Color = Color.White,
    secondaryContainerColor: Color = Grey30,
    secondaryContentColor: Color = GreyMain300,
    titleColor: Color = Color.Black,
    bodyColor: Color = Color.Black,
    captionColor: Color = GreyMain300,
    shapeRadiusDp: Int = 18,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 12.dp
) {
    // false면 아무것도 그리지 않음 (성능/의도 명확화)
    if (!visible) return

    // 바깥 영역 터치 시 onDismiss 호출
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(shapeRadiusDp.dp),
            color = containerColor,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 300.dp, max = 400.dp)   // 모바일 기준 적정 폭 한정
                    .padding(horizontal = 24.dp, vertical = 22.dp)
            ) {
                // 제목
                Text(text = title, style = AppTextStyles.b1_bold_18, color = titleColor)

                // 본문 (최대 3줄, 전달된 줄만 노출)
                Spacer(Modifier.height(12.dp))

                body1?.let {
                    Text(text = it, style = AppTextStyles.b2_medium_16, color = bodyColor)
                    Spacer(Modifier.height(6.dp))
                }
                body2?.let {
                    Text(text = it, style = AppTextStyles.b3_medium_14, color = captionColor)
                    Spacer(Modifier.height(6.dp))
                }
                body3?.let {
                    Text(text = it, style = AppTextStyles.b3_medium_14, color = captionColor)
                    Spacer(Modifier.height(6.dp))
                }

                // 버튼 영역 (우측 정렬)
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    // 보조 버튼 (회색)
                    Button(
                        onClick = onSecondaryClick,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = secondaryContainerColor,
                            contentColor = secondaryContentColor
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(secondaryButtonText, style = AppTextStyles.b3_medium_14)
                    }

                    Spacer(Modifier.width(8.dp))

                    // 기본 버튼 (보라)
                    Button(
                        onClick = onPrimaryClick,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryContainerColor,
                            contentColor = primaryContentColor
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(primaryButtonText, style = AppTextStyles.b3_medium_14)
                    }
                }
            }
        }
    }
}

/**
 * 기존 NotificationPermissionDialog 호환용 래퍼
 *
 * - 내부적으로 TwoButtonBasicDialog를 사용
 * - 기존 호출부를 변경하지 않아도 동일 동작
 */
@Composable
fun NotificationPermissionDialog(
    visible: Boolean,
    onAllow: () -> Unit,
    onLater: () -> Unit,
    onDismiss: () -> Unit
) {
    TwoButtonBasicDialog(
        visible = visible,
        title = "알림 권한 설정",
        body1 = "'누보'에서 알림을 보내고자 합니다.?",
        body2 = "알림을 켜야 영상을 저장했을 때 바로 알려드릴 수 있어요.",

        primaryButtonText = "알림 켜기",
        secondaryButtonText = "나중에",

        onPrimaryClick = onAllow,
        onSecondaryClick = onLater,
        onDismiss = onDismiss,
        containerColor = Color.White,
        tonalElevation = 0.dp
    )
}

@Composable
fun EditCardAlertDialog(
    visible: Boolean,
    onKeepEditing: () -> Unit,
    onDiscardAndExit: () -> Unit,
    onDismiss: () -> Unit
) {
    TwoButtonBasicDialog(
        visible = visible,
        title = "저장되지 않은 내용",
        body1 = "저장하지 않고 나가시겠어요?",
        body2 = "지금 나가면 수정한 내용이 사라져요.",

        primaryButtonText = "계속 편집",
        secondaryButtonText = "나가기",

        onPrimaryClick = onKeepEditing, // 편집 유지
        onSecondaryClick = onDiscardAndExit,       // 변경 폐기 후 나가기
        onDismiss = onDismiss,
        containerColor = Color.White,
        tonalElevation = 0.dp
    )
}
