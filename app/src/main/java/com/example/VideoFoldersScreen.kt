package com.example

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun VideoFoldersScreen(viewModel: MainViewModel, onVideoClick: (MediaEntity) -> Unit) {
    val publicMedia by viewModel.publicMedia.collectAsStateWithLifecycle()
    var peekItem by remember { mutableStateOf<MediaEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070F11))
    ) {
        // Custom Top Bar with colored squares
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(24.dp).background(Color(0xFF006064)))
                Box(modifier = Modifier.size(24.dp).background(Color(0xFF00ACC1)))
                Box(modifier = Modifier.size(24.dp).background(Color(0xFF424242)))
                Box(modifier = Modifier.size(24.dp).background(Color.White))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF1E1E1E)).padding(4.dp))
                Icon(Icons.Filled.Sort, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF1E1E1E)).padding(4.dp))
                Icon(Icons.Filled.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF1E1E1E)).padding(4.dp))
            }
        }

        Divider(color = Color(0xFF1E2A2E), thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Grouped by Folder", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { }) {
                    Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = Color(0xFF00FFCC), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Expand All", color = Color(0xFF00FFCC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { }) {
                    Icon(Icons.Filled.ExpandLess, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Collapse All", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        val mediaByFolder = publicMedia.groupBy { it.folderName }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            mediaByFolder.forEach { (folderName, itemsInFolder) ->
                item {
                    var isExpanded by remember { mutableStateOf(true) }
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15191C)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column {
                            // Folder Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isExpanded = !isExpanded }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Folder, contentDescription = null, tint = Color(0xFF00FFCC))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(folderName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF252A2D), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("${itemsInFolder.size} items", color = Color.Gray, fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(
                                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = null,
                                    tint = Color(0xFF00FFCC)
                                )
                            }
                            
                            // Folder Content
                            if (isExpanded) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    itemsInFolder.forEach { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .pointerInput(Unit) {
                                                    detectTapGestures(
                                                        onLongPress = {
                                                            peekItem = item
                                                        },
                                                        onTap = {
                                                            onVideoClick(item)
                                                        }
                                                    )
                                                }
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(100.dp, 60.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.Black)
                                            ) {
                                                if (item.thumbnailUri.isNotEmpty()) {
                                                    AsyncImage(
                                                        model = item.thumbnailUri,
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Icon(Icons.Filled.VideoFile, contentDescription = null, tint = Color.Gray, modifier = Modifier.align(Alignment.Center))
                                                }
                                                // MP4 badge
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopStart)
                                                        .background(Color(0xFF00FFCC), RoundedCornerShape(bottomEnd = 8.dp))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text("MP4", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                }
                                                // Duration overlay
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(topStart = 8.dp))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(formatDuration(item.durationMs), color = Color.White, fontSize = 8.sp)
                                                }
                                                // Play overlay
                                                Icon(
                                                    Icons.Filled.PlayCircle,
                                                    contentDescription = null,
                                                    tint = Color(0xFF00FFCC).copy(alpha = 0.8f),
                                                    modifier = Modifier.align(Alignment.Center).size(28.dp)
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.width(12.dp))
                                            
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(item.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(formatSize(item.fileSize), color = Color.Gray, fontSize = 10.sp)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Icon(Icons.Filled.FavoriteBorder, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                                        Icon(Icons.Filled.MoreVert, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    peekItem?.let { item ->
        PeekPreviewDialog(
            item = item,
            onDismiss = { peekItem = null },
            onLaunchFullScreen = {
                peekItem = null
                onVideoClick(item)
            }
        )
    }
}

@Composable
fun PeekPreviewDialog(item: MediaEntity, onDismiss: () -> Unit, onLaunchFullScreen: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                    .background(Color(0xFF030708))
                    .clickable(enabled = false) {}
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("✨ 3D Touch Peek Preview", color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                // Video simulated preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.thumbnailUri.isNotEmpty()) {
                        AsyncImage(
                            model = item.thumbnailUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Filled.VideoFile, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    }
                    // Muted and progress overlay
                    Box(modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.6f)).padding(4.dp)) {
                        Icon(Icons.Filled.VolumeOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    // Mini HUD
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Replay10, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                        Icon(Icons.Filled.Forward10, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }

                // File Info
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(item.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${formatSize(item.fileSize)} - ${formatDuration(item.durationMs)}", color = Color.Gray, fontSize = 12.sp)
                }
                
                // Actions
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Divider(color = Color(0xFF1E2A2E))
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onLaunchFullScreen() }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Fullscreen, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Open Full Screen Player", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Launch standard playback flow", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                    Divider(color = Color(0xFF1E2A2E))
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onDismiss() }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.FavoriteBorder, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Mark as Favorite", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Keep handy in favorites tab", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                    Divider(color = Color(0xFF1E2A2E))
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onDismiss() }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = Color.Red)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Delete File Permanently", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Erase video file from local device", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
