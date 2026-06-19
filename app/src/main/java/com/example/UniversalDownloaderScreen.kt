package com.example

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalDownloaderScreen(viewModel: MainViewModel, onMediaClick: (MediaEntity) -> Unit) {
    val activeDownloads by viewModel.activeDownloads.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var urlInput by remember { mutableStateOf("") }
    var selectedFormatIndex by remember { mutableStateOf(0) }
    var useInternalMemory by remember { mutableStateOf(true) }
    var customLocation by remember { mutableStateOf("/storage/emulated/0/Download") }

    val formatOptions = listOf(
        Triple("Best Quality", "bv*+ba/b", false),
        Triple("1080p Video", "bv*[height<=1080]+ba/b", false),
        Triple("720p Video", "bv*[height<=720]+ba/b", false),
        Triple("Audio Only", "ba/b", true),
        Triple("Smallest Size", "wv*+wa/w", false)
    )

    Scaffold(
        containerColor = Color(0xFF09090B),
        topBar = { TopBarCompound() }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Main Input Compound
            InputEngineCompound(
                urlInput = urlInput,
                onUrlChange = { urlInput = it },
                formatOptions = formatOptions,
                selectedFormatIndex = selectedFormatIndex,
                onFormatSelected = { selectedFormatIndex = it },
                useInternalMemory = useInternalMemory,
                onMemoryPreferenceChanged = { useInternalMemory = it },
                customLocation = customLocation,
                onCustomLocationChanged = { customLocation = it },
                onDownloadClick = {
                    if (urlInput.isNotBlank()) {
                        viewModel.triggerDownload(
                            url = urlInput,
                            title = "Downloaded_Media_${System.currentTimeMillis()}",
                            isAudio = formatOptions[selectedFormatIndex].third,
                            resolution = formatOptions[selectedFormatIndex].first,
                            fileSizeMb = 125.0,
                            ytDlpFormat = formatOptions[selectedFormatIndex].second,
                            customDirectory = customLocation
                        )
                        Toast.makeText(context, "Download sequence initiated", Toast.LENGTH_SHORT).show()
                        urlInput = ""
                    } else {
                        Toast.makeText(context, "Please enter a valid URL", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Active Tasks Compound
            ActiveDownloadsCompound(activeDownloads)
        }
    }
}

// ==========================================
// COMPOUNDS (Organisms)
// ==========================================

@Composable
fun TopBarCompound() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("THE CONTINENTAL", color = Color(0xFFE2E8F0), fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Text("Engine v4.2", color = Color(0xFF38BDF8), fontSize = 11.sp, fontFamily = FontFamily.Monospace, letterSpacing = 0.5.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButtonMolecule(icon = Icons.Outlined.Settings)
            IconButtonMolecule(icon = Icons.Filled.Person)
        }
    }
}

@Composable
fun InputEngineCompound(
    urlInput: String,
    onUrlChange: (String) -> Unit,
    formatOptions: List<Triple<String, String, Boolean>>,
    selectedFormatIndex: Int,
    onFormatSelected: (Int) -> Unit,
    useInternalMemory: Boolean,
    onMemoryPreferenceChanged: (Boolean) -> Unit,
    customLocation: String,
    onCustomLocationChanged: (String) -> Unit,
    onDownloadClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13141A)),
        border = BorderStroke(1.dp, Color(0xFF272A35))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("MEDIA SOURCE", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            UrlInputMolecule(urlInput, onUrlChange)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            FormatSelectorMolecule(formatOptions, selectedFormatIndex, onFormatSelected)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("DESTINATION", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            DirectorySelectorMolecule(
                useInternalMemory = useInternalMemory,
                onPreferenceChanged = onMemoryPreferenceChanged,
                customLocation = customLocation,
                onCustomLocationChanged = onCustomLocationChanged
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            PrimaryActionAtom(onClick = onDownloadClick)
        }
    }
}

@Composable
fun ActiveDownloadsCompound(activeDownloads: List<MediaEntity>) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ACTIVE QUEUE", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            BadgeAtom(text = "${activeDownloads.size} TASKS")
        }

        if (activeDownloads.isEmpty()) {
            EmptyStateMolecule()
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                activeDownloads.forEach { item ->
                    DownloadProgressMolecule(item)
                }
            }
        }
    }
}

// ==========================================
// MOLECULES
// ==========================================

@Composable
fun UrlInputMolecule(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Paste stream URL...", color = Color(0xFF475569)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF09090B),
            unfocusedContainerColor = Color(0xFF09090B),
            focusedBorderColor = Color(0xFF38BDF8),
            unfocusedBorderColor = Color(0xFF272A35),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color(0xFFE2E8F0)
        ),
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null, tint = Color(0xFF475569)) }
    )
}

@Composable
fun FormatSelectorMolecule(
    options: List<Triple<String, String, Boolean>>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF09090B)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF272A35))
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (options[selectedIndex].third) Icons.Filled.AudioFile else Icons.Filled.VideoFile,
                        contentDescription = null, 
                        tint = Color(0xFF38BDF8)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(options[selectedIndex].first, color = Color.White, fontWeight = FontWeight.Medium)
                }
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.Gray)
            }
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF13141A)).fillMaxWidth(0.85f)
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { 
                        Column {
                            Text(option.first, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(option.second, color = Color(0xFF475569), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    },
                    onClick = {
                        onSelected(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DirectorySelectorMolecule(
    useInternalMemory: Boolean,
    onPreferenceChanged: (Boolean) -> Unit,
    customLocation: String,
    onCustomLocationChanged: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChipAtom(
                selected = useInternalMemory,
                label = "Internal",
                icon = Icons.Outlined.CheckCircle,
                onClick = {
                    onPreferenceChanged(true)
                    onCustomLocationChanged("/storage/emulated/0/Download")
                },
                modifier = Modifier.weight(1f)
            )
            FilterChipAtom(
                selected = !useInternalMemory,
                label = "Custom",
                icon = Icons.Outlined.CreateNewFolder,
                onClick = {
                    onPreferenceChanged(false)
                    onCustomLocationChanged("/storage/emulated/0/Movies")
                },
                modifier = Modifier.weight(1f)
            )
        }
        
        AnimatedVisibility(visible = !useInternalMemory) {
            OutlinedTextField(
                value = customLocation,
                onValueChange = onCustomLocationChanged,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                textStyle = LocalTextStyle.current.copy(color = Color(0xFF38BDF8), fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF09090B),
                    unfocusedContainerColor = Color(0xFF09090B),
                    focusedBorderColor = Color(0xFF38BDF8).copy(alpha = 0.5f),
                    unfocusedBorderColor = Color(0xFF272A35)
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Text("Target: $customLocation", color = Color(0xFF475569), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun EmptyStateMolecule() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF13141A))
            .drawBehind { // Subtle graphic
                drawCircle(color = Color(0xFF272A35).copy(alpha=0.3f), radius = 100f, center = Offset(size.width, size.height/2f))
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Inbox, contentDescription = null, tint = Color(0xFF272A35), modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("No active operations", color = Color(0xFF475569), fontSize = 13.sp)
        }
    }
}

@Composable
fun DownloadProgressMolecule(item: MediaEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13141A)),
        border = BorderStroke(1.dp, Color(0xFF272A35))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                )
                Text("${(item.downloadProgress * 100).toInt()}%", color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF272A35))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(item.downloadProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF818CF8))))
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(item.downloadSpeedText, color = Color(0xFF475569), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("-", color = Color(0xFF475569), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// ==========================================
// ATOMS
// ==========================================

@Composable
fun IconButtonMolecule(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0xFF13141A))
            .border(1.dp, Color(0xFF272A35), CircleShape)
            .clickable { },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFFE2E8F0), modifier = Modifier.size(18.dp))
    }
}

@Composable
fun FilterChipAtom(
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF38BDF8).copy(alpha = 0.1f) else Color(0xFF09090B))
            .border(1.dp, if (selected) Color(0xFF38BDF8) else Color(0xFF272A35), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(icon, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(label, color = if (selected) Color(0xFF38BDF8) else Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun PrimaryActionAtom(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Download, contentDescription = null, tint = Color(0xFF0F172A))
            Spacer(modifier = Modifier.width(8.dp))
            Text("INITIALIZE DOWNLOAD", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun BadgeAtom(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF38BDF8).copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = Color(0xFF38BDF8), fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
}

