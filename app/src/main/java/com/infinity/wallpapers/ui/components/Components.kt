package com.infinity.wallpapers.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.infinity.wallpapers.ads.NativeAdCard
import com.infinity.wallpapers.domain.Wallpaper
import com.infinity.wallpapers.presentation.FeedViewModel

@Composable
fun shimmerBrush(): Brush {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(0f, 1200f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "x")
    val base = MaterialTheme.colorScheme.surfaceVariant
    return Brush.linearGradient(listOf(base, base.copy(alpha = 0.45f), base), Offset(x - 400f, 0f), Offset(x, 400f))
}

@Composable
fun WallpaperCard(w: Wallpaper, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var loaded by remember(w.thumbUrl) { mutableStateOf(false) }
    val brush: Brush = if (loaded) SolidColor(MaterialTheme.colorScheme.surfaceVariant) else shimmerBrush()
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(0.62f),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Box(Modifier.fillMaxSize().background(brush)) {
            AsyncImage(
                model = w.thumbUrl, contentDescription = w.title, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(), onSuccess = { loaded = true },
            )
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(w.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge)
            }
            w.badge?.let {
                Surface(
                    Modifier.align(Alignment.TopStart).padding(8.dp),
                    shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary,
                ) {
                    Text(it, Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = Color.White,
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, onSeeAll: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (onSeeAll != null) TextButton(onClick = onSeeAll) { Text("See all") }
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(32.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Icon(Icons.Filled.CloudOff, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Icon(Icons.Filled.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Retry") }
    }
}

@Composable
fun EmptyView(message: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(32.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Icon(Icons.Filled.Photo, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ShimmerGrid(modifier: Modifier = Modifier) {
    val brush = shimmerBrush()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp), modifier = modifier, userScrollEnabled = false,
        contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(8) { Box(Modifier.aspectRatio(0.62f).background(brush, RoundedCornerShape(20.dp))) }
    }
}

/** Lazy grid with pagination, native ads every 8 items, loading footer and retry. */
@Composable
fun FeedContent(
    vm: FeedViewModel,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
    showAds: Boolean = true,
    emptyText: String = "No wallpapers here yet.",
) {
    val err = vm.error
    when {
        vm.items.isEmpty() && vm.loading -> ShimmerGrid(modifier)
        vm.items.isEmpty() && err != null -> ErrorView(err, vm::retry, modifier)
        vm.items.isEmpty() -> EmptyView(emptyText, modifier)
        else -> {
            val state = rememberLazyGridState()
            val near by remember {
                derivedStateOf {
                    val info = state.layoutInfo
                    (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - 6
                }
            }
            LaunchedEffect(near, vm.items.size) { if (near) vm.loadMore() }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp), state = state, modifier = modifier,
                contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                vm.items.chunked(8).forEachIndexed { ci, chunk ->
                    items(chunk, key = { it.id }) { w -> WallpaperCard(w) { onOpen(w.id) } }
                    if (showAds && chunk.size == 8) {
                        item(key = "ad_$ci", span = { GridItemSpan(maxLineSpan) }) { NativeAdCard() }
                    }
                }
                if (vm.loading || err != null) {
                    item(key = "footer", span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                            if (err != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(err, textAlign = TextAlign.Center)
                                    TextButton(onClick = vm::retry) { Text("Retry") }
                                }
                            } else CircularProgressIndicator(Modifier.size(28.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Plain grid for local lists (favorites, downloads). */
@Composable
fun SimpleGrid(items: List<Wallpaper>, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp), modifier = modifier,
        contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items, key = { it.id }) { w -> WallpaperCard(w) { onOpen(w.id) } }
    }
}
