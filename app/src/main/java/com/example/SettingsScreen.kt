package com.example

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.BorderStroke

private val DarkBackground = Color(0xFF030708) // As per image
private val CardBackground = Color(0xFF081215)
private val BorderColor = Color(0xFF13282C)
private val TextGray = Color(0xFF7A8D92)

@Composable
fun NewSettingsScreen(viewModel: MainViewModel, context: Context, onClose: () -> Unit = {}) {
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf("CONTROLS") }
    val tabs = listOf("CONTROLS", "GESTURES", "EQUALIZER", "DISPLAY & SORT", "LIBRARY", "DOWNLOADER")

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            // Header Tabs
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Bar with tabs and close button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScrollableTabRow(
                            selectedTabIndex = tabs.indexOf(selectedTab),
                            containerColor = Color.Transparent,
                            contentColor = Color.Transparent, // hide default indicators and ink ripples
                            divider = {},
                            indicator = {},
                            edgePadding = 0.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            tabs.forEach { tab ->
                                val isSelected = selectedTab == tab
                                Tab(
                                    selected = isSelected,
                                    onClick = { selectedTab = tab },
                                    modifier = Modifier.padding(end = 12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) CyberCyan else Color.Transparent,
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = tab,
                                            color = if (isSelected) CyberCyan else TextGray,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        
                        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                    
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)

                    // LazyColumn content area
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        when (selectedTab) {
                            "CONTROLS" -> controlsTab(userSettings, viewModel.userPrefs, scope)
                            "GESTURES" -> gesturesTab(userSettings, viewModel.userPrefs, scope)
                            "EQUALIZER" -> equalizerTab(userSettings, viewModel.userPrefs, scope)
                            "DISPLAY & SORT" -> displayAndSortTab(userSettings, viewModel.userPrefs, scope)
                            "LIBRARY" -> libraryTab(userSettings, viewModel.userPrefs, scope)
                            "DOWNLOADER" -> downloaderTab(userSettings, viewModel.userPrefs, scope)
                        }
                    }
                }
            }
        }
        
        // Bottom signature
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(CardBackground.copy(alpha = 0.9f))
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("- Created By - Muhammed Bilal C", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Using - Google AI Studio", color = Color.Gray, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("\uD83D\uDCE7 MhdxBilal@proton.me", color = TextGray, fontSize = 9.sp)
                Text("✈ @MHDXBILAL7", color = TextGray, fontSize = 9.sp)
                Text("\uD83D\uDCF7 @mhdxbilal", color = TextGray, fontSize = 9.sp)
            }
        }
    }
}
