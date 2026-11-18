@file:OptIn(ExperimentalMaterial3Api::class)

package com.infinity.wallpapers.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.infinity.wallpapers.ServiceLocator
import com.infinity.wallpapers.presentation.DetailViewModel
import com.infinity.wallpapers.ui.components.ErrorView
import com.infinity.wallpapers.utils.formatCount
import com.infinity.wallpapers.wallpaper.WallTarget
import com.infinity.wallpapers.wallpaper.displayUrl

@Composable
fun detailVm(id: String): DetailViewModel =
    viewModel(key = "detail:$id", factory = viewModelFactory { initializer { DetailViewModel(ServiceLocator.repo, id) } })

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

// ---------------------------------------------------------------- 7. Wallpaper detail
@Composable
fun DetailScreen(id: String, onBack: () -> Unit, onPreview: () -> Unit) {
    val vm = detailVm(id)
    val w = vm.wallpaper
    val fav by vm.isFav.collectAsState()
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(vm) { vm.messages.collect { snack.showSnackbar(it) } }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                title = { Text(w?.title ?: "", maxLines = 1) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = onPreview, enabled = w != null) { Icon(Icons.Filled.Fullscreen, "Preview") }
                    IconButton(onClick = vm::toggleFav, enabled = w != null) {
                        Icon(
                            if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            if (fav) "Remove from favorites" else "Add to favorites",
                            tint = if (fav) Color(0xFFFF4D6D) else LocalContentColor.current,
                        )
                    }
                },
            )
        },
    ) { pad ->
        val err = vm.error
        when {
            w == null && err == null -> Box(Modifier.padding(pad).fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            w == null -> ErrorView(err ?: "Not available", vm::load, Modifier.padding(pad))
            else -> Column(
                Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Card(Modifier.fillMaxWidth().height(440.dp), shape = RoundedCornerShape(24.dp)) {
                    AsyncImage(
                        model = w.localUri ?: w.previewUrl, contentDescription = w.title,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                    )
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    w.badge?.let { AssistChip(onClick = {}, label = { Text(it) }) }
                    AssistChip(onClick = {}, label = { Text(w.resolutionLabel) })
                    if (w.category.isNotBlank()) AssistChip(onClick = {}, label = { Text(w.category) })
                }
                if (vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth())

                Text("Set as wallpaper", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.setWallpaper(WallTarget.HOME) }, enabled = !vm.busy, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Home, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Home")
                    }
                    Button(onClick = { vm.setWallpaper(WallTarget.LOCK) }, enabled = !vm.busy, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Lock, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Lock")
                    }
                    Button(onClick = { vm.setWallpaper(WallTarget.BOTH) }, enabled = !vm.busy, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Layers, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Both")
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = vm::download, enabled = !vm.busy, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Download, null); Spacer(Modifier.width(8.dp)); Text("Download")
                    }
                    OutlinedButton(onClick = vm::share, enabled = !vm.busy, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Share, null); Spacer(Modifier.width(8.dp)); Text("Share")
                    }
                }
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Information", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        InfoRow("Resolution", w.resolutionLabel)
                        InfoRow("Quality", w.badge ?: "Standard")
                        if (w.category.isNotBlank()) InfoRow("Category", w.category)
                        if (w.downloads > 0) InfoRow("Downloads", formatCount(w.downloads))
                        w.credit?.let { InfoRow("Credit", it) }
                        if (w.localUri != null) InfoRow("Saved", "On this device")
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 8. Full-screen preview
@Composable
fun PreviewScreen(id: String, onBack: () -> Unit) {
    val vm = detailVm(id)
    val w = vm.wallpaper
    var mode by remember { mutableIntStateOf(0) } // 0 plain, 1 lock, 2 home
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (w != null) {
            AsyncImage(
                model = w.displayUrl(), contentDescription = w.title, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 4f)
                            offset = if (scale == 1f) Offset.Zero else offset + pan
                        }
                    }
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
            )
            when (mode) {
                1 -> Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = 96.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("09:41", color = Color.White, fontSize = 76.sp, fontWeight = FontWeight.Light)
                    Text("Wednesday, 18 November", color = Color.White.copy(alpha = 0.9f))
                }
                2 -> Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = 96.dp, start = 24.dp, end = 24.dp, bottom = 32.dp)) {
                    repeat(5) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), Arrangement.SpaceBetween) {
                            repeat(4) { Box(Modifier.size(56.dp).background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))) }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Row(Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(28.dp)).padding(12.dp), Arrangement.SpaceEvenly) {
                        repeat(4) { Box(Modifier.size(52.dp).background(Color.White.copy(alpha = 0.35f), RoundedCornerShape(16.dp))) }
                    }
                }
            }
        } else if (vm.error != null) {
            ErrorView(vm.error ?: "", vm::load)
        } else {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
            Spacer(Modifier.weight(1f))
            listOf("Plain", "Lock", "Home").forEachIndexed { i, label ->
                FilterChip(
                    selected = mode == i, onClick = { mode = i; scale = 1f; offset = Offset.Zero },
                    label = { Text(label) }, modifier = Modifier.padding(horizontal = 3.dp),
                )
            }
        }
    }
}
