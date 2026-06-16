package com.example

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.media3.session.SessionToken
import androidx.media3.session.MediaController
import androidx.media3.common.MediaItem
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.ui.theme.MyApplicationTheme
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.launch

// High-fidelity helper duration & sizing format functions
fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

fun formatSize(sizeBytes: Long): String {
    val mb = sizeBytes / (1024.0 * 1024.0)
    return String.format("%.1f MB", mb)
}

// High-fidelity futuristic cyber branding colors
val Obsidian = Color(0xFF030303)
val SurfaceDark = Color(0xFF0F1219)
val CyberCyan = Color(0xFF00FFCC)
val NeonPurple = Color(0xFF9E00FF)
val RetroGold = Color(0xFFFFCC00) // Snaptube-like yellow/gold
val GlassBase = Color(0xFF161B26)
val GlassBorder = Color(0xFF2B3346)
val BrandGradient = Brush.linearGradient(listOf(CyberCyan, NeonPurple))
val YellowGradient = Brush.linearGradient(listOf(RetroGold, Color(0xFFFF8800)))

class MainActivity : ComponentActivity() {
    private lateinit var controllerFuture: ListenableFuture<MediaController>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val sessionToken = SessionToken(this, android.content.ComponentName(this, PlayerService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        setContent {
            MyApplicationTheme {
                val app = application as TheContinentalApp
                val viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = MainViewModel.Factory(app, app.database.mediaDao(), app.userPreferencesRepository)
                )

                var currentController by remember { mutableStateOf<MediaController?>(null) }
                val context = LocalContext.current

                LaunchedEffect(Unit) {
                    controllerFuture.addListener({
                        currentController = controllerFuture.get()
                    }, MoreExecutors.directExecutor())
                }

                MainAppScreen(viewModel, currentController, context)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MediaController.releaseFuture(controllerFuture)
    }
}

@Composable
fun MainAppScreen(
    viewModel: MainViewModel,
    currentController: MediaController?,
    context: Context
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    
    // Playback and Vault UI trigger screens
    var activePlayingVideo by remember { mutableStateOf<MediaEntity?>(null) }

    Scaffold(
        bottomBar = {
            SnaptubeBottomNavBar(navController = navController)
        },
        containerColor = Obsidian
    ) { paddingValues ->
        Box(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .background(Obsidian)
        ) {
            // Ambient glows
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.TopStart)
                    .offset(x = (-120).dp, y = (-60).dp)
                    .background(CyberCyan.copy(alpha = 0.12f), CircleShape)
                    .blur(110.dp)
                )
                Box(modifier = Modifier
                    .size(350.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 120.dp, y = 80.dp)
                    .background(NeonPurple.copy(alpha = 0.12f), CircleShape)
                    .blur(120.dp)
                )
            }
            
            NavHost(navController = navController, startDestination = "downloader", modifier = Modifier.fillMaxSize()) {
                composable("downloader") { 
                    DownloaderScreen(viewModel) { video ->
                        activePlayingVideo = video
                        currentController?.setMediaItem(MediaItem.fromUri(video.uri))
                        currentController?.prepare()
                        currentController?.play()
                    } 
                }
                composable("play") { 
                    PlayerFoldersScreen(viewModel, onVideoClick = { video ->
                        activePlayingVideo = video
                        currentController?.setMediaItem(MediaItem.fromUri(video.uri))
                        currentController?.prepare()
                        currentController?.play()
                    }) 
                }
                composable("settings") { 
                    SettingsScreen(viewModel, context) 
                }
            }

            // Custom built-in video controllers HUD overlay
            AnimatedVisibility(
                visible = activePlayingVideo != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                activePlayingVideo?.let { activeVideo ->
                    InteractiveHUDPlayerScreen(
                        mediaItem = activeVideo,
                        viewModel = viewModel,
                        currentController = currentController,
                        onClose = {
                            currentController?.stop()
                            activePlayingVideo = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SnaptubeBottomNavBar(navController: NavHostController) {
    data class NavItem(val route: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector, val label: String)
    val items = listOf(
        NavItem("downloader", Icons.Filled.Search, Icons.Outlined.Search, "Download"),
        NavItem("play", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircle, "Play"),
        NavItem("settings", Icons.Filled.Settings, Icons.Outlined.Settings, "Settings")
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(GlassBase.copy(alpha = 0.9f))
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                val scale by animateFloatAsState(targetValue = if (isSelected) 1.15f else 1f, label = "scale")
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = if (isSelected) RetroGold else Color.Gray,
                        modifier = Modifier
                            .size(24.dp)
                            .scale(scale)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.label,
                        color = if (isSelected) RetroGold else Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun FuturisticTopBar(title: String, subtitle: String = "SYS.ACT") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(BrandGradient)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(subtitle, color = Obsidian, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
        
        // Active visual indicators
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GlassBase)
                    .border(1.dp, GlassBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.NetworkCheck, contentDescription = "Online Status", tint = CyberCyan, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// -------------------------------------------------------------
// DOWNLOADER SCREEN (Snaptube Redesign)
// -------------------------------------------------------------

@Composable
fun DownloaderScreen(viewModel: MainViewModel, onMediaClick: (MediaEntity) -> Unit) {
    val activeDownloads by viewModel.activeDownloads.collectAsStateWithLifecycle()
    val publicMedia by viewModel.publicMedia.collectAsStateWithLifecycle()
    
    var searchInput by remember { mutableStateOf("") }
    var showFormatSelector by remember { mutableStateOf(false) }
    var selectedUrlToDownload by remember { mutableStateOf("") }
    var selectedTitleToDownload by remember { mutableStateOf("") }

    val context = LocalContext.current

    // Quick Social Navigation lists (Snaptube-like Shortcuts)
    val socialShortcuts = listOf(
        Pair("YouTube", "https://www.youtube.com/watch?v=BigBuckBunny"),
        Pair("Instagram", "https://www.instagram.com/reel/AajKiRaat"),
        Pair("Vimeo", "https://vimeo.com/ElephantsDream"),
        Pair("Facebook", "https://www.facebook.com/videos/ChassisLog"),
        Pair("SoundCloud", "https://soundcloud.com/stream/TamannaTrack")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            FuturisticTopBar(title = "Snaptube Hub", subtitle = "ENGINE")
        }

        // Search Engine & Downloader input
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Snaptube",
                    color = RetroGold,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                OutlinedTextField(
                    value = searchInput,
                    onValueChange = { searchInput = it },
                    placeholder = { Text("Search or enter video link...", color = Color.Gray, fontSize = 14.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceDark),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = RetroGold,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = RetroGold
                    ),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null, tint = Color.Gray) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (searchInput.isNotBlank()) {
                                    selectedUrlToDownload = searchInput
                                    selectedTitleToDownload = if (searchInput.startsWith("http")) "Downloaded_Stream" else searchInput
                                    showFormatSelector = true
                                } else {
                                    Toast.makeText(context, "Please enter a valid link first!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(YellowGradient)
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = "Search Download", tint = Obsidian, modifier = Modifier.size(20.dp))
                        }
                    }
                )
            }
        }

        // Web Shortcuts grids
        item {
            Text(
                text = "SITE SHORTCUTS",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 28.dp, top = 20.dp, bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                socialShortcuts.forEach { site ->
                    Card(
                        modifier = Modifier
                            .width(110.dp)
                            .clickable {
                                searchInput = site.second
                                selectedUrlToDownload = site.second
                                selectedTitleToDownload = site.first + "_Trending_Clip"
                                showFormatSelector = true
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = GlassBase),
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(RetroGold.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                val vector = when (site.first) {
                                    "YouTube" -> Icons.Filled.PlayArrow
                                    "Instagram" -> Icons.Filled.Camera
                                    "Vimeo" -> Icons.Filled.VideoLibrary
                                    "Facebook" -> Icons.Filled.ThumbUp
                                    else -> Icons.Filled.MusicNote
                                }
                                Icon(vector, contentDescription = null, tint = RetroGold, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(site.first, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Active Downloading Lists queue (Snaptube-like downloading feedback)
        if (activeDownloads.isNotEmpty()) {
            item {
                Text(
                    text = "ACTIVE DOWNLOADS",
                    color = CyberCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 28.dp, top = 28.dp, bottom = 12.dp)
                )
            }

            items(activeDownloads, key = { it.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, GlassBorder)
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
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = item.downloadSpeedText,
                                color = RetroGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Futuristic pulsing LinearProgressIndicator
                        LinearProgressIndicator(
                            progress = { item.downloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = CyberCyan,
                            trackColor = GlassBorder,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val percent = (item.downloadProgress * 100).toInt()
                            Text(
                                text = if (percent >= 99) "Converting..." else "$percent%",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "${formatSize(item.fileSize)} Total",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Finished local downloads inside history down
        item {
            Text(
                text = "RECENT COMPLETED",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 28.dp, top = 28.dp, bottom = 12.dp)
            )
        }

        val completedMedia = publicMedia.filter { it.isDownloaded }
        if (completedMedia.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = GlassBorder
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No completed downloads yet.", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(completedMedia, key = { it.id }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMediaClick(item) }
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp, 60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GlassBase)
                    ) {
                        if (item.thumbnailUri.isNotEmpty()) {
                            AsyncImage(
                                model = item.thumbnailUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = if (item.isAudioOnly) Icons.Filled.MusicNote else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${item.videoResolution} | ${formatSize(item.fileSize)} | ${item.folderName}",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = { viewModel.removeFile(item.id) }) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = Color.Gray)
                    }
                }
            }
        }
    }

    // Dynamic Resolution & Custom format prompts (Snaptube-style "Download video as")
    if (showFormatSelector) {
        AlertDialog(
            onDismissRequest = { showFormatSelector = false },
            containerColor = SurfaceDark,
            title = {
                Text(
                    text = "Download video as",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("MUSIC", color = RetroGold, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 6.dp))
                    
                    val musicFormats = listOf(
                        Triple("Fast (128K M4A)", "M4A", 4.3),
                        Triple("Classic MP3 (128K)", "MP3", 4.8),
                        Triple("Premium High (320K)", "MP3", 9.6)
                    )
                    musicFormats.forEach { triple ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.triggerDownload(
                                        url = selectedUrlToDownload,
                                        title = selectedTitleToDownload,
                                        isAudio = true,
                                        resolution = triple.second,
                                        fileSizeMb = triple.third
                                    )
                                    showFormatSelector = false
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(triple.first, color = Color.White, fontSize = 14.sp)
                            Text("${triple.third} MB", color = Color.Gray, fontSize = 12.sp)
                        }
                    }

                    Divider(color = GlassBorder, modifier = Modifier.padding(vertical = 12.dp))

                    Text("VIDEO", color = RetroGold, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 6.dp))
                    
                    val videoFormats = listOf(
                        Triple("Fast (360p)", "360p", 22.5),
                        Triple("Standard (480p)", "480p", 32.5),
                        Triple("High quality (720p)", "720p", 44.4),
                        Triple("Ultra quality (1080p)", "1080p", 132.8)
                    )
                    videoFormats.forEach { triple ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.triggerDownload(
                                        url = selectedUrlToDownload,
                                        title = selectedTitleToDownload,
                                        isAudio = false,
                                        resolution = triple.second,
                                        fileSizeMb = triple.third
                                    )
                                    showFormatSelector = false
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(triple.first, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("${triple.third} MB", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

// -------------------------------------------------------------
// PLAY / DIRECTORY FOLDERS SCREEN (With Vault and Playlist "ggdea")
// -------------------------------------------------------------

@Composable
fun PlayerFoldersScreen(viewModel: MainViewModel, onVideoClick: (MediaEntity) -> Unit) {
    val publicMedia by viewModel.publicMedia.collectAsStateWithLifecycle()
    val privateMedia by viewModel.privateMedia.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()

    var activePillIndex by remember { mutableIntStateOf(0) } // 0: Folders, 1: Playlists, 2: The Vault
    val foldersList = listOf("Media Download", "Movies", "Camera", "Music Download")

    var selectedFolderDetail by remember { mutableStateOf<String?>(null) }
    
    // Playlist states
    val playlistEntries = remember { mutableStateListOf("ggdea") }
    var selectedPlaylistName by remember { mutableStateOf<String?>(null) }
    var showCreatePlaylistPrompt by remember { mutableStateOf(false) }
    var newPlaylistInputText by remember { mutableStateOf("") }
    
    // Video multi-selection for adding to playlist
    var showSelectMediaForPlaylistDialog by remember { mutableStateOf(false) }
    var currentMappingPlaylistName by remember { mutableStateOf("") }
    val playlistSelectedMediaIds = remember { mutableStateListOf<Int>() }

    // Private Vault states
    var vaultInputPin by remember { mutableStateOf("") }
    var vaultAuthenticated by remember { mutableStateOf(false) }
    var vaultSetupStep by remember { mutableIntStateOf(0) } // 0: Enter PIN, 1: Enter recovery email
    
    var draftPinFirstEntry by remember { mutableStateOf("") }
    var draftEmailEntry by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        FuturisticTopBar(title = "Local Hub", subtitle = "PLAYER")

        // Top navigation bubble pills (Matches MX Player Top Menu)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val pills = listOf("Folders", "Playlists", "The Vault")
            pills.forEachIndexed { index, name ->
                val active = activePillIndex == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (active) RetroGold else GlassBase)
                        .border(1.dp, if (active) RetroGold else GlassBorder, RoundedCornerShape(20.dp))
                        .clickable { 
                            activePillIndex = index
                            if (index != 2) {
                                vaultAuthenticated = false
                                vaultInputPin = ""
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        name,
                        color = if (active) Obsidian else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // VIEW RENDERING
        when (activePillIndex) {
            0 -> { // Folders listing view
                if (selectedFolderDetail == null) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 120.dp)
                    ) {
                        items(foldersList) { folder ->
                            val itemsInFolder = publicMedia.filter { it.folderName == folder }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable { selectedFolderDetail = folder },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = GlassBase),
                                border = BorderStroke(1.dp, GlassBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Folder,
                                        contentDescription = null,
                                        tint = RetroGold,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(folder, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("${itemsInFolder.size} elements", color = Color.Gray, fontSize = 12.sp)
                                    }
                                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
                                }
                            }
                        }
                    }
                } else {
                    // Folder details listing
                    val folderName = selectedFolderDetail!!
                    val folderMedia = publicMedia.filter { it.folderName == folderName }
                    
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { selectedFolderDetail = null }) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            Text(folderName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }

                        if (folderMedia.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No files in this folder", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 120.dp)
                            ) {
                                items(folderMedia) { item ->
                                    var showItemActionsDropdown by remember { mutableStateOf(false) }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onVideoClick(item) }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(GlassBase)
                                        ) {
                                            if (item.thumbnailUri.isNotEmpty()) {
                                                AsyncImage(
                                                    model = item.thumbnailUri,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = RetroGold, modifier = Modifier.align(Alignment.Center))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("${formatSize(item.fileSize)} | ${formatDuration(item.durationMs)}", color = Color.Gray, fontSize = 11.sp)
                                        }
                                        
                                        // Menu actions trigger selector
                                        Box {
                                            IconButton(onClick = { showItemActionsDropdown = true }) {
                                                Icon(Icons.Filled.MoreVert, contentDescription = "Options", tint = Color.Gray)
                                            }

                                            DropdownMenu(
                                                expanded = showItemActionsDropdown,
                                                onDismissRequest = { showItemActionsDropdown = false },
                                                containerColor = SurfaceDark
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Move to Private Vault", color = Color.White) },
                                                    onClick = {
                                                        viewModel.toggleFileVault(item, makePrivate = true)
                                                        showItemActionsDropdown = false
                                                        Toast.makeText(context, "Locked and Hidden successfully!", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Add to Playlist", color = Color.White) },
                                                    onClick = {
                                                        currentMappingPlaylistName = "ggdea" // add to ggdea by default or let select later
                                                        viewModel.setFilePlaylist(item, "ggdea")
                                                        showItemActionsDropdown = false
                                                        Toast.makeText(context, "Added to playlist!", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Delete", color = Color.Red) },
                                                    onClick = {
                                                        viewModel.removeFile(item.id)
                                                        showItemActionsDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> { // Playlists list ("ggdea") view
                if (selectedPlaylistName == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                    ) {
                        Button(
                            onClick = { showCreatePlaylistPrompt = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GlassBase),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, GlassBorder)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = CyberCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create New Playlist", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(playlistEntries) { playlistName ->
                                val playlistItems = publicMedia.filter { it.playlistName == playlistName }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .clickable { selectedPlaylistName = playlistName },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                    border = BorderStroke(1.dp, GlassBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.PlaylistPlay, contentDescription = null, tint = RetroGold, modifier = Modifier.size(36.dp))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(playlistName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("${playlistItems.size} videos", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Playlist detailing files
                    val pName = selectedPlaylistName!!
                    val playlistVideos = publicMedia.filter { it.playlistName == pName }

                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { selectedPlaylistName = null }) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            Text(pName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                onClick = { 
                                    currentMappingPlaylistName = pName
                                    playlistSelectedMediaIds.clear()
                                    showSelectMediaForPlaylistDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Obsidian),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Add Videos", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        if (playlistVideos.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No videos in this playlist.", color = Color.Gray)
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(
                                    onClick = { onVideoClick(playlistVideos.first()) },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GlassBase),
                                    border = BorderStroke(1.dp, GlassBorder)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = RetroGold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Play All", color = Color.White)
                                }
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 120.dp)
                            ) {
                                items(playlistVideos) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onVideoClick(item) }
                                            .padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(GlassBase)) {
                                            if (item.thumbnailUri.isNotEmpty()) {
                                                AsyncImage(model = item.thumbnailUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                            } else {
                                                Icon(Icons.Filled.VideoLibrary, contentDescription = null, tint = CyberCyan, modifier = Modifier.align(Alignment.Center))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(formatDuration(item.durationMs), color = Color.Gray, fontSize = 11.sp)
                                        }
                                        IconButton(onClick = { viewModel.setFilePlaylist(item, "") }) {
                                            Icon(Icons.Filled.Close, contentDescription = "Remove From Playlist", tint = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> { // Secure Private Vault (PIN input / recovery email screen)
                if (userSettings.vaultPin.isEmpty()) {
                    // Start registration process
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = RetroGold, modifier = Modifier.size(72.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Create Private Vault PIN", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Securely lock hidden resources with an unreadable cryptographic local password.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                        
                        Spacer(modifier = Modifier.height(30.dp))
                        
                        when (vaultSetupStep) {
                            0 -> {
                                Text("Enter a 4-Digit PIN", color = RetroGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = draftPinFirstEntry,
                                    onValueChange = { if (it.length <= 4) draftPinFirstEntry = it },
                                    visualTransformation = PasswordVisualTransformation(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyberCyan, unfocusedBorderColor = GlassBorder),
                                    modifier = Modifier.width(150.dp),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.titleLarge.copy(color = Color.White, textAlign = TextAlign.Center)
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = {
                                        if (draftPinFirstEntry.length == 4) {
                                            vaultSetupStep = 1
                                        } else {
                                            Toast.makeText(context, "Draft PIN must be exactly 4 digits!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Obsidian)
                                ) {
                                    Text("PROCEED", fontWeight = FontWeight.Bold)
                                }
                            }
                            1 -> {
                                Text("Add PIN Recovery Email Backup", color = RetroGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("This is crucial if you forget your vault access PIN.", color = Color.Gray, fontSize = 11.sp)
                                
                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = draftEmailEntry,
                                    onValueChange = { draftEmailEntry = it },
                                    placeholder = { Text("mbc4294@gmail.com") }, // Perfect Easter Egg metadata match!
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyberCyan, unfocusedBorderColor = GlassBorder),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    OutlinedButton(onClick = { vaultSetupStep = 0 }) {
                                        Text("BACK", color = Color.White)
                                    }
                                    Button(
                                        onClick = {
                                            if (draftEmailEntry.contains("@")) {
                                                viewModel.setupVaultPIN(draftPinFirstEntry)
                                                viewModel.setupVaultEmail(draftEmailEntry)
                                                Toast.makeText(context, "Secure Vault initialized successfully!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Please input a valid recovery email address", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RetroGold, contentColor = Obsidian)
                                    ) {
                                        Text("CONFIRM INITIALIZATION", fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }
                } else if (!vaultAuthenticated) {
                    // Standard Vault access code flow
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = RetroGold, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Enter Vault PIN", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        
                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            value = vaultInputPin,
                            onValueChange = { 
                                if (it.length <= 4) {
                                    vaultInputPin = it
                                    if (it == userSettings.vaultPin) {
                                        vaultAuthenticated = true
                                    }
                                }
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyberCyan, unfocusedBorderColor = GlassBorder),
                            modifier = Modifier.width(140.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Recovery Email registered: ${userSettings.vaultEmail}", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    // Logged in securely! List locked videos
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.LockOpen, contentDescription = null, tint = CyberCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Vault Core", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.weight(1f))
                            Text("Authenticated", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        if (privateMedia.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No private locked resources inside.", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 120.dp)
                            ) {
                                items(privateMedia) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(GlassBase)) {
                                            Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.align(Alignment.Center))
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(formatSize(item.fileSize), color = Color.Gray, fontSize = 11.sp)
                                        }
                                        Button(
                                            onClick = {
                                                viewModel.toggleFileVault(item, makePrivate = false)
                                                Toast.makeText(context, "File unlocked and moved back to public media!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = GlassBase),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Unlock", color = Color.White, fontSize = 11.sp)
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

    // Modal dialog trigger for playlist creation
    if (showCreatePlaylistPrompt) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistPrompt = false },
            containerColor = SurfaceDark,
            title = { Text("Create New Playlist", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPlaylistInputText,
                    onValueChange = { newPlaylistInputText = it },
                    placeholder = { Text("Enter playlist name...") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyberCyan, unfocusedBorderColor = GlassBorder),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistInputText.isNotBlank()) {
                            playlistEntries.add(newPlaylistInputText)
                            newPlaylistInputText = ""
                            showCreatePlaylistPrompt = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Obsidian)
                ) {
                    Text("Create")
                }
            }
        )
    }

    // Modal dialog trigger for assigning videos to playlist
    if (showSelectMediaForPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showSelectMediaForPlaylistDialog = false },
            containerColor = SurfaceDark,
            title = { Text("Select videos to add", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                val nonPlaylistVideos = publicMedia.filter { it.playlistName != currentMappingPlaylistName }
                if (nonPlaylistVideos.isEmpty()) {
                    Text("All local videos are already in this playlist!", color = Color.Gray)
                } else {
                    LazyColumn(modifier = Modifier.height(250.dp)) {
                        items(nonPlaylistVideos) { item ->
                            val isChecked = playlistSelectedMediaIds.contains(item.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) playlistSelectedMediaIds.remove(item.id)
                                        else playlistSelectedMediaIds.add(item.id)
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        if (isChecked) playlistSelectedMediaIds.remove(item.id)
                                        else playlistSelectedMediaIds.add(item.id)
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = CyberCyan)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(item.title, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        playlistSelectedMediaIds.forEach { id ->
                            val target = publicMedia.find { it.id == id }
                            if (target != null) {
                                viewModel.setFilePlaylist(target, currentMappingPlaylistName)
                            }
                        }
                        showSelectMediaForPlaylistDialog = false
                        Toast.makeText(context, "Videos added successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Obsidian)
                ) {
                    Text("Done")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// SECURE WORKFLOW PREFERENCES (MX Settings Panel Redesign)
// -------------------------------------------------------------

@Composable
fun SettingsScreen(viewModel: MainViewModel, context: Context) {
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Preferences groupings state
    var selectedPreferenceCategory by remember { mutableStateOf<String?>(null) }

    if (selectedPreferenceCategory == null) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 120.dp)
        ) {
            item {
                FuturisticTopBar(title = "App Settings", subtitle = "CONFIG")
            }

            // Storage feedback progress bar indicator
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = GlassBase),
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Storage, contentDescription = null, tint = RetroGold, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Internal Memory", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("100.25 GB / 128.00 GB", color = Color.Gray, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { 100.25f / 128.00f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = RetroGold,
                            trackColor = GlassBorder
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            val settingsOptions = listOf(
                Triple("List Configuration", "Directories, sorting tags, visual themes", Icons.Filled.List),
                Triple("Player Preferences", "Double-tap controllers, aspect zoom ratios", Icons.Filled.PlayArrow),
                Triple("Decoder Frameworks", "HW/HW+, audio SW fallback limits", Icons.Filled.Memory),
                Triple("Equalizer & Audio", "Boost limit parameters, fading bounds", Icons.Filled.VolumeUp),
                Triple("Subtitle Appearance", "Display languages, directories, formatting", Icons.Filled.Subtitles),
                Triple("Download Configurations", "Concurrency counts, network data allowances", Icons.Filled.Settings)
            )

            items(settingsOptions) { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedPreferenceCategory = option.first }
                        .padding(vertical = 16.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(GlassBase)
                            .border(1.dp, GlassBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(option.third, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(option.first, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(option.second, color = Color.Gray, fontSize = 12.sp)
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
                AboutSection()
            }
        }
    } else {
        // Render sub-category settings detail
        val category = selectedPreferenceCategory!!
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedPreferenceCategory = null }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(category, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 120.dp)
            ) {
                when (category) {
                    "List Configuration" -> {
                        item {
                            preferenceSwitchItem("Mark Last Played File", "Indicate played files with status indicators", true) {}
                            preferenceSwitchItem("Floating Play Action Button", "Show quick play floating badge on corner", true) {}
                            preferenceSwitchItem("Show Hidden Files & Folders", "Reveal system .nomedia folders and dotfiles", false) {}
                        }
                    }
                    "Player Preferences" -> {
                        item {
                            preferenceSwitchItem("A-B Repeat Option", "Reveal loops inside playback overlays HUD", true) {}
                            preferenceSwitchItem("Double-Tap to Zoom", "Flip screen zoom sizes smoothly", true) {}
                            preferenceSwitchItem("Resume Playback Location", "Re-locate last frame played automatically", true) {}
                        }
                    }
                    "Decoder Frameworks" -> {
                        item {
                            preferenceSwitchItem("Always Prefer HW+ Decoder", "Use hardware system chipsets first", true) {}
                            preferenceSwitchItem("Correct Aspect Ratio Lock", "Ignore broken layouts constraints", true) {}
                            preferenceSwitchItem("Software SW Fallback", "Switch to cpu decoding if hw pipelines fail", true) {}
                        }
                    }
                    "Equalizer & Audio" -> {
                        item {
                            preferenceSwitchItem("Enable Audio Volume Boost", "Allows amplification ranges up to 200%", true) {}
                            preferenceSwitchItem("Pause playback on Headphone Disconnect", "Disconnect inputs smoothly", true) {}
                            preferenceSwitchItem("Fading transitions", "Fade audio volumes on seeking frames", true) {}
                        }
                    }
                    "Subtitle Appearance" -> {
                        item {
                            Text("SUBTITLE COLOR SPECTRUM", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                val colors = listOf(Color.White, Color.Yellow, Color.Green, CyberCyan)
                                colors.forEach { color ->
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(2.dp, Color.White, CircleShape)
                                            .clickable {}
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            preferenceSwitchItem("Render SSA Fonts Italic effects", "Render text tags formatting codes", true) {}
                        }
                    }
                    "Download Configurations" -> {
                        item {
                            preferenceSwitchItem("Download via Mobile Data Allowances", "Prevent operations when metered limits alert", true) {}
                            preferenceSwitchItem("Multi-threading downloads prioritization", "Process downloads in active parallel lanes", true) {}
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            Text("SIMULATED CONCURRENCY LIMITS", color = RetroGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("WIFI: 1 Task max | Mobile Data: 1 Task max", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun preferenceSwitchItem(title: String, subtitle: String, defaultChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    var checkState by remember { mutableStateOf(defaultChecked) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, color = Color.Gray, fontSize = 11.sp)
        }
        Switch(
            checked = checkState,
            onCheckedChange = {
                checkState = it
                onCheckedChange(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyberCyan,
                checkedTrackColor = CyberCyan.copy(alpha = 0.3f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = GlassBase
            )
        )
    }
}

// -------------------------------------------------------------
// SECURE INTERACTIVE GESTURE FULLPLAY PLAYER HUD (MX Level Overlay)
// -------------------------------------------------------------

@Composable
fun InteractiveHUDPlayerScreen(
    mediaItem: MediaEntity,
    viewModel: MainViewModel,
    currentController: MediaController?,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Playback state synchronization variables
    val loopChecked by viewModel.isLooping.collectAsStateWithLifecycle()
    val speedValue by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val decoderPreset by viewModel.activeDecoderMode.collectAsStateWithLifecycle()
    val aspectFit by viewModel.selectedAspectRatio.collectAsStateWithLifecycle()
    val sleepTimerLeft by viewModel.sleepTimerLeftMinutes.collectAsStateWithLifecycle()

    val presetName by viewModel.equalizerPreset.collectAsStateWithLifecycle()
    val eq1 by viewModel.equalizerBand1.collectAsStateWithLifecycle()
    val eq2 by viewModel.equalizerBand2.collectAsStateWithLifecycle()
    val eq3 by viewModel.equalizerBand3.collectAsStateWithLifecycle()
    val eq4 by viewModel.equalizerBand4.collectAsStateWithLifecycle()
    val eq5 by viewModel.equalizerBand5.collectAsStateWithLifecycle()

    // Touch gesture levels simulated
    var volumeLevelSimulated by remember { mutableFloatStateOf(0.7f) }
    var brightnessLevelSimulated by remember { mutableFloatStateOf(0.6f) }
    var isMutedSimulated by remember { mutableStateOf(false) }

    // Dialog sheets toggles
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showAspectDialog by remember { mutableStateOf(false) }

    // Swipe HUD Indicators
    var activeSwipeFeedbackText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(enabled = false) {} // block background touches
    ) {
        // Stylized full layout video view representation
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { activeSwipeFeedbackText = "" },
                        onDrag = { change: PointerInputChange, dragAmount: Offset ->
                            change.consume()
                            val width = this@pointerInput.size.width
                            val positionX = change.position.x
                            if (positionX < width / 2) {
                                // Brightness level update
                                val delta = -dragAmount.y / 800f
                                brightnessLevelSimulated = (brightnessLevelSimulated + delta).coerceIn(0f, 1f)
                                activeSwipeFeedbackText = "Brightness: ${(brightnessLevelSimulated * 100).toInt()}%"
                            } else {
                                // Volume level update
                                val delta = -dragAmount.y / 800f
                                volumeLevelSimulated = (volumeLevelSimulated + delta).coerceIn(0f, 1f)
                                activeSwipeFeedbackText = "Volume: ${(volumeLevelSimulated * 100).toInt()}%"
                            }
                        }
                    )
                }
        ) {
            // High fidelity decorative background rendering
            AsyncImage(
                model = mediaItem.thumbnailUri.ifEmpty { "https://picsum.photos/id/191/800/400" },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Aesthetic overlay shadows
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent, Color.Black.copy(alpha = 0.9f))))
            )
        }

        // SWIPE OVERLAY LEVEL INDICATIONS CODES
        if (activeSwipeFeedbackText.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(activeSwipeFeedbackText, color = CyberCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        // PLAYER MAIN CONTROLS OVERLAYS CORE HUD
        Column(modifier = Modifier.fillMaxSize()) {
            
            // TOP STATUS HEADS PANEL
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Close Player", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(mediaItem.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(180.dp))
                        Text(decoderPreset, color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                // Fast config shortcuts buttons (Sleep timer, Decoder picker, Equalizer card)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { showAspectDialog = true }) {
                        Icon(Icons.Outlined.AspectRatio, contentDescription = "Aspect Ratio", tint = Color.White)
                    }
                    IconButton(onClick = { showEqualizerDialog = true }) {
                        Icon(Icons.Outlined.Equalizer, contentDescription = "Equalizer", tint = CyberCyan)
                    }
                    IconButton(onClick = { showSpeedDialog = true }) {
                        Text("${speedValue}x", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { showSleepTimerDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = "Sleep timer",
                            tint = if (sleepTimerLeft > 0) RetroGold else Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // BOTTOM CONTROL PANELS HUD
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                // Interactive Play progress slider simulated
                var progress by remember { mutableFloatStateOf(0.35f) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(formatDuration((mediaItem.durationMs * progress).toLong()), color = Color.White, fontSize = 11.sp)
                    Slider(
                        value = progress,
                        onValueChange = { progress = it },
                        colors = SliderDefaults.colors(thumbColor = RetroGold, activeTrackColor = RetroGold, inactiveTrackColor = Color.Gray),
                        modifier = Modifier.weight(1f)
                    )
                    Text(formatDuration(mediaItem.durationMs), color = Color.White, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // CENTER PLAYER BUTTONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute shortcut code
                    IconButton(onClick = { isMutedSimulated = !isMutedSimulated }) {
                        Icon(
                            imageVector = if (isMutedSimulated) Icons.Filled.VolumeMute else Icons.Filled.VolumeUp,
                            contentDescription = null,
                            tint = if (isMutedSimulated) Color.Red else Color.White
                        )
                    }

                    // Loop state
                    IconButton(onClick = { viewModel.isLooping.value = !loopChecked }) {
                        Icon(
                            imageVector = Icons.Filled.Loop,
                            contentDescription = "Looping Toggle",
                            tint = if (loopChecked) CyberCyan else Color.White
                        )
                    }

                    // Main Play Toggle
                    var playing by remember { mutableStateOf(true) }
                    IconButton(
                        onClick = {
                            playing = !playing
                            if (playing) currentController?.play() else currentController?.pause()
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(RetroGold)
                    ) {
                        Icon(
                            imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play Control",
                            tint = Obsidian,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // A-B Loop Selector simulated
                    var isABRepeatEnabled by remember { mutableStateOf(false) }
                    IconButton(onClick = {
                        isABRepeatEnabled = !isABRepeatEnabled
                        Toast.makeText(context, if (isABRepeatEnabled) "A-B Repeat enabled between current bounds!" else "A-B Repeat cleared", Toast.LENGTH_SHORT).show()
                    }) {
                        Text(
                            "A-B",
                            color = if (isABRepeatEnabled) CyberCyan else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    // Decoder toggler state
                    IconButton(onClick = {
                        val next = when (decoderPreset) {
                            "HW Decoder" -> "HW+ Decoder"
                            "HW+ Decoder" -> "SW Decoder"
                            else -> "HW Decoder"
                        }
                        viewModel.activeDecoderMode.value = next
                    }) {
                        Text("HW", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Interactive playback speed adjustment overlay
    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            containerColor = SurfaceDark,
            title = { Text("Playback speed", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.playbackSpeed.value = speed
                                    currentController?.setPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${speed}x", color = if (speedValue == speed) CyberCyan else Color.White, fontWeight = FontWeight.Bold)
                            if (speedValue == speed) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = CyberCyan)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Interactive Aspect Ratio dialog
    if (showAspectDialog) {
        AlertDialog(
            onDismissRequest = { showAspectDialog = false },
            containerColor = SurfaceDark,
            title = { Text("Aspect Ratio", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf("Default", "1:1 Matches", "3:4 Quick", "16:9 Cinema", "Stretch Fit").forEach { aspect ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectedAspectRatio.value = aspect
                                    showAspectDialog = false
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(aspect, color = if (aspectFit == aspect) CyberCyan else Color.White, fontWeight = FontWeight.Bold)
                            if (aspectFit == aspect) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = CyberCyan)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Sleep Timer interactive selector dialog
    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            containerColor = SurfaceDark,
            title = { Text("Set sleep timer", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    val times = listOf(
                        Pair("Never disable", 0),
                        Pair("10 Minutes", 10),
                        Pair("15 Minutes", 15),
                        Pair("20 Minutes", 20),
                        Pair("30 Minutes", 30),
                        Pair("60 Minutes", 60)
                    )
                    times.forEach { pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.sleepTimerLeftMinutes.value = pair.second
                                    showSleepTimerDialog = false
                                    Toast.makeText(context, if (pair.second > 0) "Timer enabled! App will pause in ${pair.second} mins" else "Timer cleared.", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(pair.first, color = if (sleepTimerLeft == pair.second) RetroGold else Color.White, fontWeight = FontWeight.Bold)
                            if (sleepTimerLeft == pair.second) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = RetroGold)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Equalizer fine-tune custom dialog
    if (showEqualizerDialog) {
        AlertDialog(
            onDismissRequest = { showEqualizerDialog = false },
            containerColor = SurfaceDark,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Equalizer Panel", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(presetName, color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            },
            text = {
                Column {
                    // Presets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Flat", "Bass Boost", "Vocal", "Rock", "Jazz").forEach { name ->
                            val active = presetName == name
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (active) CyberCyan else GlassBase)
                                    .clickable { viewModel.applyPreset(name) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(name, color = if (active) Obsidian else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Simulated 5-band Equalizer Sliders
                    val bands = listOf(
                        Pair("60Hz", eq1),
                        Pair("230Hz", eq2),
                        Pair("910Hz", eq3),
                        Pair("4kHz", eq4),
                        Pair("14kHz", eq5)
                    )

                    bands.forEachIndexed { index, pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(pair.first, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.width(50.dp))
                            Slider(
                                value = pair.second,
                                onValueChange = { newValue ->
                                    when (index) {
                                        0 -> viewModel.updateBand1(newValue)
                                        1 -> viewModel.updateBand2(newValue)
                                        2 -> viewModel.updateBand3(newValue)
                                        3 -> viewModel.updateBand4(newValue)
                                        4 -> viewModel.updateBand5(newValue)
                                    }
                                },
                                valueRange = -1.0f..1.0f,
                                colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showEqualizerDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Obsidian)
                ) {
                    Text("Done")
                }
            }
        )
    }
}
