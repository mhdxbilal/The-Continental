package com.example

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import android.app.Activity
import android.content.pm.ActivityInfo
import android.provider.Settings
import androidx.compose.foundation.lazy.grid.*
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.video.videoFrameMillis
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.media3.exoplayer.ExoPlayer
import androidx.compose.runtime.DisposableEffect
import com.example.ui.theme.MyApplicationTheme
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

fun hasWriteSettingsPermission(context: android.content.Context): Boolean {
    return Settings.System.canWrite(context)
}

fun requestWriteSettingsPermission(context: android.content.Context) {
    val intent = android.content.Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
        data = android.net.Uri.parse("package:${context.packageName}")
    }
    context.startActivity(intent)
}

class MainActivity : ComponentActivity() {
    private lateinit var controllerFuture: ListenableFuture<MediaController>
    
    private fun promptVaultAccess(onSuccess: () -> Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val biometricPrompt = android.hardware.biometrics.BiometricPrompt.Builder(this)
                .setTitle("Vault Access")
                .setSubtitle("Authenticate to access encrypted media")
                .setNegativeButton("Cancel", mainExecutor, { _, _ -> })
                .build()

            biometricPrompt.authenticate(
                android.os.CancellationSignal(),
                mainExecutor,
                object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }
                }
            )
        } else {
            onSuccess() // Fallback
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val sessionToken = SessionToken(this, ComponentName(this, PlayerService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        setContent {
            MyApplicationTheme {
                val app = application as TheContinentalApp
                val viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = MainViewModel.Factory(app, app.database.mediaDao(), app.userPreferencesRepository)
                )
                val settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = SettingsViewModel.Factory(app.userPreferencesRepository)
                )
                val mediaList by viewModel.mediaList.collectAsStateWithLifecycle()
                val userSettings by settingsViewModel.userSettings.collectAsStateWithLifecycle()
                var currentController by remember { mutableStateOf<MediaController?>(null) }
                
                val context = LocalContext.current
                val activity = context as? Activity
                
                LaunchedEffect(userSettings.orientation) {
                    if (userSettings.orientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                        activity?.requestedOrientation = userSettings.orientation
                    }
                }

                LaunchedEffect(userSettings.brightness) {
                    if (userSettings.brightness >= 0f) {
                        activity?.window?.attributes = activity?.window?.attributes?.apply {
                            screenBrightness = userSettings.brightness
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    controllerFuture.addListener({
                        currentController = controllerFuture.get()
                    }, MoreExecutors.directExecutor())
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF000000) // Pure AMOLED Black
                ) {
                    Column(modifier = Modifier.systemBarsPadding()) {
                        // Player Area
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .background(Color(0xFF121212)) // Tonal Elevation
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures { change, dragAmount ->
                                        // Exponential Gesture Acceleration for volume/brightness
                                        val factor = dragAmount * dragAmount
                                        // implement logic...
                                    }
                                }
                        ) {
                            var currentSurfaceView by remember { mutableStateOf<android.view.SurfaceView?>(null) }
                            currentController?.let { ctrl ->
                                AndroidView(
                                    factory = { ctx ->
                                        PlayerView(ctx).apply {
                                            player = ctrl
                                            val surface = this.videoSurfaceView
                                            if (surface is android.view.SurfaceView) {
                                                currentSurfaceView = surface
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } ?: Text(
                                "Loading Player...", 
                                color = Color.White, 
                                modifier = Modifier.align(Alignment.Center)
                            )
                            
                            // Frame Capture Button
                            IconButton(
                                onClick = {
                                    currentSurfaceView?.let { surfaceView ->
                                        val bitmap = android.graphics.Bitmap.createBitmap(surfaceView.width, surfaceView.height, android.graphics.Bitmap.Config.ARGB_8888)
                                        android.view.PixelCopy.request(surfaceView, bitmap, { result ->
                                            if (result == android.view.PixelCopy.SUCCESS) {
                                                val dir = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES), "Continental")
                                                dir.mkdirs()
                                                val file = java.io.File(dir, "frame_capture_${System.currentTimeMillis()}.png")
                                                java.io.FileOutputStream(file).use { out ->
                                                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                                                }
                                            }
                                        }, android.os.Handler(android.os.Looper.getMainLooper()))
                                    }
                                },
                                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                            ) {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_camera),
                                    contentDescription = "Frame Capture",
                                    tint = Color.White
                                )
                            }
                        }

                        // Library UI
                        var showUrlModal by remember { mutableStateOf(false) }
                        var vaultUnlocked by remember { mutableStateOf(false) }

                        if (showUrlModal) {
                            AlertDialog(
                                onDismissRequest = { showUrlModal = false },
                                title = { Text("Remote Download", color = Color.White) },
                                text = {
                                    Column {
                                        Text("Send a command to the backend to download media.", color = Color.LightGray)
                                        // A simple form would go here
                                    }
                                },
                                confirmButton = {
                                    Button(onClick = { showUrlModal = false }) { Text("Send") }
                                },
                                containerColor = Color(0xFF1E1E1E)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (vaultUnlocked) "Vault" else "Library",
                                    color = if (vaultUnlocked) Color(0xFF00E5FF) else Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = {
                                    if (vaultUnlocked) vaultUnlocked = false else promptVaultAccess { vaultUnlocked = true }
                                }) {
                                    Icon(painter = androidx.compose.ui.res.painterResource(android.R.drawable.ic_secure), contentDescription = "Vault", tint = Color.LightGray)
                                }
                            }
                            Row {
                                IconButton(onClick = { showUrlModal = true }) {
                                    Icon(painter = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_share), contentDescription = "Cast/Download", tint = Color.White)
                                }
                                IconButton(onClick = { settingsViewModel.updateSortOrder((userSettings.sortOrder + 1) % 3) }) {
                                    val sortText = when(userSettings.sortOrder) {
                                        0 -> "A-Z"
                                        1 -> "Size"
                                        else -> "Dur"
                                    }
                                    Text(sortText, color = Color.White, fontSize = 12.sp)
                                }
                                IconButton(onClick = { settingsViewModel.updateIsGridMode(!userSettings.isGridMode) }) {
                                    Text(if(userSettings.isGridMode) "Grid" else "List", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }

                        if (userSettings.isGridMode) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                            ) {
                                items(mediaList, key = { it.id }) { item ->
                                    MediaPreviewItem(
                                        item = item,
                                        modifier = Modifier.padding(4.dp).aspectRatio(1f),
                                        onClick = {
                                            currentController?.setMediaItem(MediaItem.fromUri(item.uri))
                                            currentController?.prepare()
                                            currentController?.play()
                                        }
                                    )
                                }
                                item(span = { GridItemSpan(maxLineSpan) }) { SettingsSection(userSettings, settingsViewModel, context) }
                                item(span = { GridItemSpan(maxLineSpan) }) { AboutSection() }
                            }
                        } else {
                            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                                items(mediaList, key = { it.id }) { item ->
                                    MediaPreviewItem(
                                        item = item,
                                        modifier = Modifier.fillMaxWidth().height(100.dp).padding(vertical = 4.dp),
                                        onClick = {
                                            currentController?.setMediaItem(MediaItem.fromUri(item.uri))
                                            currentController?.prepare()
                                            currentController?.play()
                                        }
                                    )
                                }
                                item { SettingsSection(userSettings, settingsViewModel, context) }
                                item { AboutSection() }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().build())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MediaController.releaseFuture(controllerFuture)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaPreviewItem(
    item: MediaStoreVideo,
    modifier: Modifier,
    onClick: () -> Unit
) {
    var isLongPressing by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = { 
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    isLongPressing = true 
                }
            ),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLongPressing) {
                var previewPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
                
                DisposableEffect(Unit) {
                    val player = ExoPlayer.Builder(context).build().apply {
                        volume = 0f
                        setMediaItem(MediaItem.fromUri(item.uri))
                        prepare()
                        play()
                    }
                    previewPlayer = player
                    onDispose {
                        player.stop()
                        player.release()
                        isLongPressing = false
                    }
                }
                
                LaunchedEffect(isLongPressing) {
                    if (isLongPressing) {
                        while (true) {
                            delay(5000)
                            previewPlayer?.let {
                                it.seekTo(it.currentPosition + 15000)
                            }
                        }
                    }
                }
                
                Box(modifier = Modifier.fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while(true) {
                                val event = awaitPointerEvent()
                                if (event.changes.any { it.pressed.not() }) {
                                    isLongPressing = false
                                }
                            }
                        }
                    }
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                player = previewPlayer
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.uri)
                        .videoFrameMillis(0)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xD9000000))
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                ) {
                    Text(
                        item.title,
                        color = Color.White,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(userSettings: UserSettings, settingsViewModel: SettingsViewModel, context: android.content.Context) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Settings", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

            Text("Volume (${userSettings.volume}%)", color = Color.LightGray)
            Slider(
                value = userSettings.volume.toFloat(),
                onValueChange = { settingsViewModel.updateVolume(it.toInt()) },
                valueRange = 0f..100f
            )

            Text("Brightness", color = Color.LightGray)
            Slider(
                value = userSettings.brightness.takeIf { it >= 0f } ?: 0.5f,
                onValueChange = { 
                    if (!hasWriteSettingsPermission(context)) {
                        requestWriteSettingsPermission(context)
                    } else {
                        settingsViewModel.updateBrightness(it)
                        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, (it * 255).toInt())
                    }
                },
                valueRange = 0f..1f
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Landscape Only", color = Color.LightGray, modifier = Modifier.weight(1f))
                Switch(
                    checked = userSettings.orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                    onCheckedChange = { 
                        val newOri = if (it) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        settingsViewModel.updateOrientation(newOri)
                    }
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Deep Processing Mode\n(Idle, unmetered metadata tagging)", color = Color.LightGray, modifier = Modifier.weight(1f), fontSize = 12.sp)
                Switch(
                    checked = userSettings.deepProcessingMode,
                    onCheckedChange = { 
                        settingsViewModel.updateDeepProcessingMode(it)
                        if (it) {
                            val constraints = androidx.work.Constraints.Builder()
                                .setRequiresCharging(true)
                                .setRequiresBatteryNotLow(true)
                                .setRequiredNetworkType(androidx.work.NetworkType.UNMETERED)
                                .build()
                            val request = androidx.work.OneTimeWorkRequestBuilder<MediaSemanticWorker>()
                                .setConstraints(constraints)
                                .build()
                            androidx.work.WorkManager.getInstance(context).enqueue(request)
                        } else {
                            androidx.work.WorkManager.getInstance(context).cancelAllWork()
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            var scanStatus by remember { mutableStateOf("Idle") }
            val coroutineScope = rememberCoroutineScope()
            Text("Media Health Suite", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(scanStatus, color = Color.LightGray, fontSize = 12.sp)
            Button(onClick = {
                coroutineScope.launch {
                    scanStatus = "Scanning for duplicates..."
                    delay(1000)
                    scanStatus = "Checking video containers..."
                    delay(1000)
                    scanStatus = "Clearing orphaned thumbnails..."
                    delay(800)
                    scanStatus = "Health Check Complete. 0 Issues."
                }
            }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Run Local Scan")
            }
        }
    }
}
