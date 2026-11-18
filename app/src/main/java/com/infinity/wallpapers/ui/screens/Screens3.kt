@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.infinity.wallpapers.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.imageLoader
import com.infinity.wallpapers.BuildConfig
import com.infinity.wallpapers.ServiceLocator
import com.infinity.wallpapers.ads.AdsManager
import com.infinity.wallpapers.domain.WallpaperQuery
import com.infinity.wallpapers.presentation.SearchViewModel
import com.infinity.wallpapers.ui.components.*
import com.infinity.wallpapers.utils.findActivity
import com.infinity.wallpapers.utils.isOnline
import kotlinx.coroutines.launch

// ---------------------------------------------------------------- 9. Favorites
@Composable
fun FavoritesScreen(onOpen: (String) -> Unit, onDownloads: () -> Unit) {
    val items by remember { ServiceLocator.repo.favorites() }.collectAsState(emptyList())
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorites", style = MaterialTheme.typography.titleLarge) },
                actions = { IconButton(onClick = onDownloads) { Icon(Icons.Filled.Download, "Downloads") } },
            )
        },
    ) { pad ->
        if (items.isEmpty()) EmptyView("No favorites yet.\nTap the heart on any wallpaper to save it here.", Modifier.padding(pad))
        else SimpleGrid(items, onOpen, Modifier.padding(pad))
    }
}

// ---------------------------------------------------------------- 10. Downloads
@Composable
fun DownloadsScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val items by remember { ServiceLocator.repo.downloads() }.collectAsState(emptyList())
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (items.isNotEmpty()) IconButton(onClick = { confirm = true }) { Icon(Icons.Filled.Delete, "Clear history") }
                },
            )
        },
    ) { pad ->
        if (items.isEmpty()) EmptyView("Nothing downloaded yet.\nDownloaded wallpapers also open offline.", Modifier.padding(pad))
        else SimpleGrid(items, onOpen, Modifier.padding(pad))
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Clear download history?") },
            text = { Text("This only clears the list in the app. Files stay in your gallery.") },
            confirmButton = { TextButton(onClick = { scope.launch { ServiceLocator.repo.clearDownloadHistory() }; confirm = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } },
        )
    }
}

// ---------------------------------------------------------------- 11. Search
private val trendingSearches = listOf("Nature", "AMOLED", "Supercars", "Galaxy", "Minimalist", "Anime", "Luxury", "Mountains", "Dark", "Flowers")

@Composable
fun SearchScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val vm: SearchViewModel = viewModel(factory = viewModelFactory { initializer { SearchViewModel(ServiceLocator.repo) } })
    var text by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf("") }
    val recentSearches by vm.recents.collectAsState()
    val viewed by remember { ServiceLocator.repo.recents() }.collectAsState(emptyList())
    val focus = LocalFocusManager.current
    fun go(q: String) {
        text = q; submitted = q.trim()
        if (submitted.isNotEmpty()) vm.submit(submitted)
        focus.clearFocus()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = text, onValueChange = { text = it; if (it.isBlank()) submitted = "" },
                        placeholder = { Text("Search wallpapers or categories") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { go(text) }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { if (text.isNotEmpty()) IconButton(onClick = { text = ""; submitted = "" }) { Icon(Icons.Filled.Close, "Clear") } },
            )
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            if (submitted.isNotEmpty()) {
                FeedContent(feedVm("search:$submitted", WallpaperQuery(search = submitted)), onOpen, showAds = false,
                    emptyText = "No wallpapers found for \"$submitted\".")
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (recentSearches.isNotEmpty()) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Recent searches", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                TextButton(onClick = vm::clear) { Text("Clear") }
                            }
                        }
                        item {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                recentSearches.forEach { q ->
                                    AssistChip(onClick = { go(q) }, label = { Text(q) }, leadingIcon = { Icon(Icons.Filled.History, null, Modifier.size(18.dp)) })
                                }
                            }
                        }
                    }
                    item { Text("Trending searches", style = MaterialTheme.typography.titleMedium) }
                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            trendingSearches.forEach { q -> SuggestionChip(onClick = { go(q) }, label = { Text(q) }) }
                        }
                    }
                    if (viewed.isNotEmpty()) {
                        item { Text("Recently viewed", style = MaterialTheme.typography.titleMedium) }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(viewed, key = { it.id }) { w -> WallpaperCard(w, Modifier.width(120.dp)) { onOpen(w.id) } }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 12. Settings
@Composable
fun SettingsScreen(onPrivacy: () -> Unit, onAbout: () -> Unit) {
    val ctx = LocalContext.current
    val s = ServiceLocator.settings
    val theme by s.theme.collectAsState()
    val quality by s.quality.collectAsState()
    val wifiOnly by s.wifiOnly.collectAsState()
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var cacheBytes by remember { mutableLongStateOf(ctx.imageLoader.diskCache?.size ?: 0L) }
    fun say(m: String) { scope.launch { snack.showSnackbar(m) } }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        topBar = { TopAppBar(title = { Text("Settings", style = MaterialTheme.typography.titleLarge) }) },
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            SettingsHeader("Appearance")
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("system" to "System default", "light" to "Light", "dark" to "Dark").forEach { (k, label) ->
                    FilterChip(selected = theme == k, onClick = { s.setTheme(k) }, label = { Text(label) })
                }
            }
            SettingsHeader("Image quality")
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = quality == "standard", onClick = { s.setQuality("standard") }, label = { Text("Standard (saves data)") })
                FilterChip(selected = quality == "high", onClick = { s.setQuality("high") }, label = { Text("High (original)") })
            }
            Text(
                "Applies to previews and when setting a wallpaper. Downloads always save the full-resolution file.",
                Modifier.padding(16.dp, 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SettingsHeader("Downloads")
            ListItem(
                headlineContent = { Text("Download on Wi-Fi only") },
                supportingContent = { Text("Saved to Pictures/Infinity Wallpapers") },
                trailingContent = { Switch(checked = wifiOnly, onCheckedChange = { s.setWifiOnly(it) }) },
            )
            SettingsHeader("Storage")
            ListItem(
                headlineContent = { Text("Clear image cache") },
                supportingContent = { Text("Cache size: ${"%.1f".format(cacheBytes / 1048576.0)} MB") },
                modifier = Modifier.clickable {
                    ctx.imageLoader.diskCache?.clear(); ctx.imageLoader.memoryCache?.clear()
                    cacheBytes = ctx.imageLoader.diskCache?.size ?: 0L
                    say("Cache cleared")
                },
            )
            ListItem(headlineContent = { Text("Clear recently viewed") },
                modifier = Modifier.clickable { scope.launch { ServiceLocator.repo.clearRecents() }; say("Recently viewed cleared") })
            ListItem(headlineContent = { Text("Clear search history") },
                modifier = Modifier.clickable { scope.launch { ServiceLocator.repo.clearSearches() }; say("Search history cleared") })

            SettingsHeader("Support")
            ListItem(
                headlineContent = { Text("Support the developer") },
                supportingContent = { Text("Watch an optional ad. Nothing in the app depends on it.") },
                modifier = Modifier.clickable { ctx.findActivity()?.let { a -> AdsManager.showRewarded(a) { say(it) } } },
            )
            if (AdsManager.privacyOptionsRequired(ctx)) {
                ListItem(
                    headlineContent = { Text("Ad privacy choices") },
                    modifier = Modifier.clickable { ctx.findActivity()?.let { AdsManager.showPrivacyOptions(it) } },
                )
            }
            SettingsHeader("About")
            ListItem(headlineContent = { Text("Privacy Policy") }, trailingContent = { Icon(Icons.Filled.ChevronRight, null) }, modifier = Modifier.clickable(onClick = onPrivacy))
            ListItem(headlineContent = { Text("About Infinity 8K Wallpapers") }, trailingContent = { Icon(Icons.Filled.ChevronRight, null) }, modifier = Modifier.clickable(onClick = onAbout))
            ListItem(
                headlineContent = { Text("Share this app") },
                modifier = Modifier.clickable {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Check out Infinity 8K Wallpapers: https://play.google.com/store/apps/details?id=${ctx.packageName}")
                    }
                    ctx.startActivity(Intent.createChooser(send, "Share app"))
                },
            )
            ListItem(headlineContent = { Text("Version") }, supportingContent = { Text("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})") })
        }
    }
}

@Composable
private fun SettingsHeader(title: String) {
    Text(title, Modifier.padding(16.dp, 20.dp, 16.dp, 8.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

// ---------------------------------------------------------------- 13. Privacy policy
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val text = remember { runCatching { ctx.assets.open("privacy_policy.txt").bufferedReader().use { it.readText() } }.getOrDefault("Privacy policy unavailable.") }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium)
            if (BuildConfig.PRIVACY_POLICY_URL.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Button(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PRIVACY_POLICY_URL))) }) { Text("Open online version") }
            }
        }
    }
}

// ---------------------------------------------------------------- 14. About
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("∞", fontSize = 72.sp, color = MaterialTheme.colorScheme.primary)
            Text("Infinity 8K Wallpapers", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text("Version ${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Text(
                "A wallpaper app with one-tap home/lock screen setup, favorites, downloads and offline access. " +
                    "The 8K and 4K badges are shown only when the original file's real pixel size qualifies.",
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text("Content source: ${ServiceLocator.repo.sourceName}", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Photos are provided by their respective owners under the licence stated by the source " +
                    "(for example Pexels, with photographer credit shown on each wallpaper). Built with Jetpack Compose, Coil, Room, WorkManager, Firebase and Google AdMob.",
                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (BuildConfig.SUPPORT_EMAIL.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${BuildConfig.SUPPORT_EMAIL}"))) }) {
                    Icon(Icons.Filled.Email, null); Spacer(Modifier.width(8.dp)); Text("Contact support")
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 15. Offline / error
@Composable
fun OfflineScreen(onRetry: () -> Unit, onDownloads: () -> Unit) {
    val ctx = LocalContext.current
    var tried by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(32.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Icon(Icons.Filled.CloudOff, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text("You're offline", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            if (tried) "Still no connection. Check Wi-Fi or mobile data and try again." else "Connect to the internet to browse wallpapers. Your downloads are still available.",
            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = { if (isOnline(ctx)) onRetry() else tried = true }) { Icon(Icons.Filled.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Retry") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onDownloads) { Text("View downloads") }
    }
}
