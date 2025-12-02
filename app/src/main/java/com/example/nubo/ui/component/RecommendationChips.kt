package com.example.nubo.ui.component

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.nubo.model.home.RecommendChipItem
import com.example.nubo.ui.theme.AppTextStyles
import com.example.nubo.ui.theme.DefaultText
import com.example.nubo.ui.theme.GreyMain100
import com.example.nubo.ui.theme.Purple50
import com.example.nubo.ui.theme.Purple700
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Brush
import com.example.nubo.data.model.CardResponse
import com.example.nubo.ui.theme.PurpleMain500
import kotlinx.coroutines.launch

@Composable
fun RecommendationChipsRow(
    chips:List<RecommendChipItem>,
    onChipClick:(RecommendChipItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxWidth()){
        LazyRow(
            state = listState,
            modifier = modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ){
            itemsIndexed(chips) {
                    index, chip ->
                RecommendationChip(
                    chip = chip,
                    onClick = {
                        onChipClick(chip)
                        coroutineScope.launch {
                            //아이템들의 위치 정보
                            val layoutInfo = listState.layoutInfo
                            //클릭된 칩에 해당하는 item 정보
                            val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull{it.index == index}

                            //칩이 보이는 경우, 중앙 정렬 애니메이션
                            itemInfo?.let{
                                val viewportCenter = layoutInfo.viewportEndOffset /2
                                val itemCenter = it.offset + it.size /2
                                val scrollDeeded = itemCenter - viewportCenter
                                listState.animateScrollBy(
                                    scrollDeeded.toFloat(),
                                    animationSpec = tween(durationMillis = 300))
                            }?: listState.animateScrollToItem(index, scrollOffset = -100)  //칩이 안보이는 경우 스크롤
                        }
                    }
                )
            }
        }

        //오른쪽 흐림 효과(페이드 아웃)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.White),
                    )
                )
                .align(Alignment.CenterEnd)
                .width(35.dp)
        )
    }
}

@Composable
private fun RecommendationChip(
    chip:RecommendChipItem,
    onClick:() -> Unit,
    modifier: Modifier = Modifier
){
    val backgroundColor = if(chip.isSelected){
        Purple50
    }else{
        Color.White
    }

    val textColor = if(chip.isSelected){
        Purple700
    }else{
        DefaultText
    }

    val borderColor = if(chip.isSelected){
        Purple700
    }else{
        GreyMain100
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape =RoundedCornerShape(20.dp)
            )
            .clickable{onClick()}
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ){
        Text(
            text = chip.title,
            color = textColor,
            style = if(chip.isSelected) AppTextStyles.label_SemiBold_12 else  AppTextStyles.label_medium_12,
            textAlign = TextAlign.Center
        )

    }

}
