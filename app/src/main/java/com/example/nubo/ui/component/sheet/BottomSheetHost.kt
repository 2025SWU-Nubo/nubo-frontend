package com.example.nubo.ui.component.sheet


import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetHost(
    route: SheetRoute?,                     // which sheet to show
    onDismiss: () -> Unit,                  // dismiss handler
    onGoCreateBoard: () -> Unit,            // AddMenu -> CreateBoard
    onGoInvite: () -> Unit,                 // CreateBoard -> Invite
    onCreateBoard: (String, Boolean) -> Unit,// create board action
    onInvite: (String) -> Unit,             // invite action
    onGoAddVideo: () -> Unit,  // add video action
    modifier: Modifier = Modifier
) {
    if (route == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier
    ) {
        when (route) {
            SheetRoute.AddMenu -> AddMenuSheet(
                onClose = onDismiss,
                onVideoClick = onGoAddVideo,
                onBoardClick = onGoCreateBoard
            )
            SheetRoute.CreateBoard -> CreateBoardSheet(
                onClose = onDismiss,
                onInviteClick = onGoInvite,
                onCreate = onCreateBoard
            )
            SheetRoute.Invite -> InviteSheet(
                onClose = onDismiss,
                onInvite = onInvite
            )
            SheetRoute.AddVideo -> AddVideoSheet(
                onClose = onDismiss
            )
        }
    }
}
