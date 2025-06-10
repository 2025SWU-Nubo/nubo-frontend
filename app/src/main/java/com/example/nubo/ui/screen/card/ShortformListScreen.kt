package com.example.nubo.ui.screen.card

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.nubo.model.card.CardDetailDialogItem
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import com.example.nubo.ui.component.DetailCardDialog


@Composable
fun ShortformListScreen(items: List<CardDetailDialogItem>) {
    var selectedItem by remember { mutableStateOf<CardDetailDialogItem?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn {
            items(items) { item ->
                Button(
                    onClick = { selectedItem = item },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text(text = item.title)
                }
            }
        }

        selectedItem?.let { item ->
            DetailCardDialog(
                item = item,
                onDismiss = { selectedItem = null }
            )
        }
    }
}

