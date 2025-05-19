package com.example.nubo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import com.example.nubo.ui.theme.GreyMain300
import com.example.nubo.ui.theme.Purple200
import androidx.compose.ui.Alignment
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon


@Composable
fun MyBoardScreen() {
    var selectedTab by remember { mutableStateOf(0) } // 카드탭 상태 기억

    Column(modifier = Modifier.fillMaxSize()) {
        TabHeader(
            selectedTabIndex = selectedTab,         // 현재 선택된 탭 인덱스
            onTabSelected = { selectedTab = it }    // 탭 클릭 시 상태 업데이트
        )

        TitleBar()
        FilterButtons()

        if (selectedTab == 0) {
            CardContent()
        } else {
            // BoardContent() 또는 다른 콘텐츠
        }
    }
}


@Composable
fun TabHeader(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("영상", "보드")

    Column(modifier = Modifier.padding(top = 30.dp)) {
        // 탭 전체 중앙 정렬
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(15.dp), // 탭 간 간격
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = index == selectedTabIndex
                    val textColor = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondary

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onTabSelected(index) } // 탭 클릭 처리
                    ) {
                        // 텍스트 설정
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor
                        )

                        Spacer(modifier = Modifier.height(4.dp)) // 텍스트-인디케이터  간격

                        // 인디케이터 설정 (선택여부에 따라 색상 다르게)
                        Box(
                            modifier = Modifier
                                .width(45.dp)
                                .height(1.dp)
                                .background(textColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TitleBar() {
    Column(modifier = Modifier.padding(top = 30.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 나의 보드 텍스트
            androidx.compose.material3.Text(
                text = "나의 보드",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            // 검색 버튼
            androidx.compose.material3.Icon(
                painter = painterResource(id = R.drawable.ic_search),
                contentDescription = "검색 아이콘",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun FilterButtons() {
    val filters = listOf("정렬", "즐겨찾기", "공유", "개인")
    var selected by remember { mutableStateOf<String?>(null) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        filters.forEach { label ->
            val isSelected = selected == label
            // 필터 버튼 클릭 시
            OutlinedButton(
                onClick = { selected = if (isSelected) null else label },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) Purple200 else Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                //필터 버튼 설정
                shape = RoundedCornerShape(7.dp),
                border = BorderStroke(1.dp, GreyMain300),
                modifier = Modifier
                    .defaultMinSize(minHeight = 32.dp) // 버튼 높이 조절
                    .height(32.dp)
                    .wrapContentWidth(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                if (label == "즐겨찾기") {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_star), // 별 아이콘 리소스 연결
                            contentDescription = "즐겨찾기",
                            modifier = Modifier.size(18.dp),

                            )
                        Spacer(modifier = Modifier.width(4.dp))
                } else {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
