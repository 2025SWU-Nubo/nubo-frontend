package com.example.nubo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.databinding.adapters.AdapterViewBindingAdapter.OnItemSelected
import com.example.nubo.ui.theme.AppFonts
import com.example.nubo.ui.theme.NuboAppTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent{
            NuboAppTheme{
                HomeScreen()
            }

        }
    }
}

@Composable
fun HomeScreen(){

    val selectedIndex = remember { mutableStateOf(0) }

    Scaffold (
        bottomBar = {BottomNavBar(
            selectedIndex = selectedIndex.value,
            onItemSelected = {selectedIndex.value = it}
        )}
    ){  innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.White),

            contentPadding = PaddingValues(bottom = 60.dp)
        ){
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item { RecentBoardSection() }
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item { RecommendedCardsSection() }
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item { RecommendedVideosSection() }
        }

    }
}

@Composable
fun RecentBoardSection(){
    val categories = listOf("엔터테인먼트","AI 및 개발","기초 디자인","요리 레시피")

    Column(modifier = Modifier.padding(horizontal =  16.dp)) {
        Text(text = "최근 본 보드", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow {
            items(categories){ category ->
                Box(
                    modifier = Modifier
                        .padding(end=8.dp)
                        .size(100.dp,100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Gray),
                    contentAlignment = Alignment.Center
                ){
                    Text(text = category, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun RecommendedCardsSection(){
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ){
            Text(text = "추천 학습 카드", style = MaterialTheme.typography.titleMedium)
            Text(text = "더보기", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(8.dp))

        repeat(2){
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(text = "추천 학습 카드명",style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "추천 학습 카드에 대한 텍스트 요약 일부 내용",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun RecommendedVideosSection(){
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(text = "미시청/추천 영상", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxHeight()
                .height(400.dp)
        ) {
            items(8){
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEFEFEF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(selectedIndex: Int =0, onItemSelected: (Int)-> Unit = {}) {
    NavigationBar {
        NavigationBarItem(icon = { Icon(
            painter = painterResource(
                id = if (selectedIndex == 0) R.drawable.nav_home_selected else R.drawable.nav_home_unselected
            ),
            contentDescription = "홈",
            tint = MaterialTheme.colorScheme.primary
        ) },
            selected = selectedIndex == 0,
            onClick = {onItemSelected(0)})

        NavigationBarItem(icon = { Icon(
            painter = painterResource(
                id = if (selectedIndex == 1) R.drawable.nav_dashboard_selected else R.drawable.nav_dashboard_unselected
            ),
            contentDescription = "나의 보드",
            tint = MaterialTheme.colorScheme.primary
        ) },
            selected = selectedIndex == 1,
            onClick = {onItemSelected(1)})

        NavigationBarItem(icon = { Icon(
            painter = painterResource(
                id = if (selectedIndex == 2) R.drawable.nav_add_selected else R.drawable.nav_add_unselected
            ),
            contentDescription = "컨텐츠 추가",
            tint = MaterialTheme.colorScheme.primary
        ) },
            selected = selectedIndex == 2,
            onClick = {onItemSelected(2)})

        NavigationBarItem(icon = { Icon(
            painter = painterResource(
                id = if (selectedIndex == 3) R.drawable.nav_book_selected else R.drawable.nav_book_unselected
            ),
            contentDescription = "학습 공간",
            tint = MaterialTheme.colorScheme.primary
        ) },
            selected = selectedIndex == 3,
            onClick = {onItemSelected(3)})

        NavigationBarItem(icon = { Icon(
            painter = painterResource(
                id = if (selectedIndex == 4) R.drawable.nav_profile_selected else R.drawable.nav_profile_unselected
            ),
            contentDescription = "마이페이지",
            tint = MaterialTheme.colorScheme.primary
        ) },
            selected = selectedIndex == 4,
            onClick = {onItemSelected(4)})
    }
}



