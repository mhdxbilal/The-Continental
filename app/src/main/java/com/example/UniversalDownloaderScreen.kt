package com.example

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun UniversalDownloaderScreen(viewModel: MainViewModel, onMediaClick: (MediaEntity) -> Unit) {
    val activeDownloads by viewModel.activeDownloads.collectAsStateWithLifecycle()
    var urlInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    var isPreferredLocation by remember { mutableStateOf(false) }

    // Format selection state
    var showFormatDropdown by remember { mutableStateOf(false) }
    val formatOptions = listOf(
        Triple("Best Quality", "bv*+ba/b", false),
        Triple("1080p Video", "bv*[height<=1080]+ba/b", false),
        Triple("720p Video", "bv*[height<=720]+ba/b", false),
        Triple("Audio Only", "ba/b", true),
        Triple("Smallest Size", "wv*+wa/w", false)
    )
    var selectedFormatIndex by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070F11)) // Match darker theme
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
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

        Text(
            text = "MHDXBILAL DOWNLOAD ENGINE",
            color = Color(0xFF00FFCC),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 24.dp, bottom = 12.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1517)),
            border = BorderStroke(1.dp, Color(0xFF1E2A2E))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Paste Media Stream / Video URL", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        placeholder = { Text("Paste URL here...", color = Color.DarkGray, fontSize = 14.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E2022),
                            unfocusedContainerColor = Color(0xFF1E2022),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    
                    Box(
                        modifier = Modifier
                            .height(56.dp)
                            .border(1.dp, Brush.linearGradient(listOf(Color(0xFF00FFCC), Color(0xFF9E00FF))), RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F1517))
                            .clickable { showFormatDropdown = true }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(formatOptions[selectedFormatIndex].first, color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color(0xFF00FFCC))
                        }
                        DropdownMenu(
                            expanded = showFormatDropdown,
                            onDismissRequest = { showFormatDropdown = false },
                            modifier = Modifier.background(Color(0xFF1E2022))
                        ) {
                            formatOptions.forEachIndexed { index, option ->
                                DropdownMenuItem(
                                    text = { 
                                        Column {
                                            Text(option.first, color = Color.White, fontWeight = FontWeight.Bold)
                                            Text("yt-dlp: ${option.second}", color = Color.Gray, fontSize = 10.sp)
                                        }
                                    },
                                    onClick = {
                                        selectedFormatIndex = index
                                        showFormatDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .border(1.dp, Color(0xFF00FFCC), RoundedCornerShape(20.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Internal Memory", color = Color.Gray, fontSize = 11.sp)
                        }
                        Box(
                            modifier = Modifier
                                .border(1.dp, Brush.horizontalGradient(listOf(Color(0xFF00FFCC), Color(0xFF9E00FF))), RoundedCornerShape(20.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Choose Custom...", color = Color(0xFF00FFCC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = isPreferredLocation,
                            onCheckedChange = { isPreferredLocation = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FFCC), uncheckedThumbColor = Color.Gray),
                            modifier = Modifier.scale(0.7f)
                        )
                        Text("Set as preferred location", color = Color.Gray, fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Target URI: /storage/emulated/0/Download", color = Color(0xFF00FFCC), fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)

                Spacer(modifier = Modifier.height(32.dp))

                if (activeDownloads.isNotEmpty()) {
                    val activeDownloadItem = activeDownloads.first()
                    val progress = activeDownloadItem.downloadProgress
                    Text("${(progress * 100).toInt()}% COMPLETE", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E2A2E))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFF00FFCC), Color(0xFF9E00FF), Color(0xFFFF007F))))
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(activeDownloadItem.downloadSpeedText, color = Color.Gray, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                } else {
                    Text("0% COMPLETE", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E2A2E))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Ready for connections...", color = Color.Gray, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (urlInput.isNotBlank()) {
                            viewModel.triggerDownload(
                                url = urlInput,
                                title = "Downloaded_Stream_${System.currentTimeMillis()}",
                                isAudio = formatOptions[selectedFormatIndex].third,
                                resolution = formatOptions[selectedFormatIndex].first,
                                fileSizeMb = 125.0,
                                ytDlpFormat = formatOptions[selectedFormatIndex].second
                            )
                            Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
                            urlInput = ""
                        } else {
                            Toast.makeText(context, "Please enter URL", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("DOWNLOAD", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
