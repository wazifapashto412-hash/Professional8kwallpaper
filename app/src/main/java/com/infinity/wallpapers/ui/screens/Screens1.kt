@file:OptIn(ExperimentalMaterial3Api::class)

package com.infinity.wallpapers.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.infinity.wallpapers.ServiceLocator
import com.infinity.wallpapers.ads.BannerAd
import com.infinity.wallpapers.ads.NativeAdCard
import com.infinity.wallpapers.domain.*
import com.infinity.wallpapers.presentation.FeedViewModel
import com.infinity.wallpapers.presentation.HomeViewModel
import com.infinity.wallpapers.ui.components.*
import com.infinity.wallpapers.utils.isOnline
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun feedVm(key: String, q: WallpaperQuery): FeedViewModel =
    viewModel(key = key, factory = viewModelFactory { initializer { FeedViewModel(ServiceLocator.repo, q) } })

// ---------------------------------------------------------------- 1. Splash
@Composable
fun SplashScreen(onDone: (String) -> Unit) {
    val ctx = LocalContext.current
    val scale = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, tween(700))
        delay(600)
        onDone(
            when {
                !ServiceLocator.settings.onboarded -> "onboarding"
                !isOnline(ctx) -> "offline"
                else -> "home"
            }
        )
    }
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0A0A0F), Color(0xFF2A2350)))),
        Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(scale.value)) {
            Text("∞", fontSize = 96.sp, color = Color(0xFF8B7BFF))
            Text("Infinity 8K Wallpapers", style = MaterialTheme.typography.titleLarge, color = Color.White)
        }
    }
}

// ---------------------------------------------------------------- 2. Onboarding
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    data class P(val icon: ImageVector, val title: String, val body: String)
    val pages = listOf(
        P(Icons.Filled.Photo, "Stunning wallpapers", "Browse high-resolution wallpapers, including 4K and 8K where the original file supports it."),
        P(Icons.Filled.PhoneAndroid, "One-tap setup", "Set any wallpaper on your home screen, lock screen or both - no extra permissions needed."),
        P(Icons.Filled.Favorite, "Make it yours", "Save favorites, download for offline use and get recommendations based on what you view."),
    )
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.End) { TextButton(onClick = onDone) { Text("Skip") } }
        HorizontalPager(pager, Modifier.weight(1f)) { i ->
            Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                Icon(pages[i].icon, null, Modifier.size(120.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(24.dp))
                Text(pages[i].title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(pages[i].body, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.Center) {
            repeat(pages.size) { i ->
                Box(
                    Modifier.padding(4.dp).size(if (pager.currentPage == i) 12.dp else 8.dp).background(
                        if (pager.currentPage == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        CircleShape,
                    )
                )
            }
        }
        Button(
            onClick = { if (pager.currentPage == pages.size - 1) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(if (pager.currentPage == pages.size - 1) "Get started" else "Next") }
    }
}

// ---------------------------------------------------------------- 3. Home
@Composable
fun HomeScreen(onOpen: (String) -> Unit, onSearch: () -> Unit, onSeeAll: (String) -> Unit) {
    val vm: HomeViewModel = viewModel(factory = viewModelFactory { initializer { HomeViewModel(ServiceLocator.repo) } })
    val recents by remember { ServiceLocator.repo.recents() }.collectAsState(emptyList())
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Infinity 8K", style = MaterialTheme.typography.titleLarge) },
                actions = { IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, "Search") } },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                val err = vm.error
                when {
                    vm.loading && vm.sections.isEmpty() -> ShimmerGrid()
                    err != null -> ErrorView(err, vm::load)
                    else -> LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                        itemsIndexed(vm.sections) { i, s ->
                            Column {
                                SectionHeader(s.title, s.slug?.let { slug -> { onSeeAll(slug) } })
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    androidx.compose.foundation.lazy.items(s.items, key = { it.id }) { w ->
                                        WallpaperCard(w, Modifier.width(if (i == 0) 220.dp else 140.dp)) { onOpen(w.id) }
                                    }
                                }
                                if (i == 1) NativeAdCard(Modifier.padding(16.dp))
                            }
                        }
                        if (recents.isNotEmpty()) {
                            item {
                                Column {
                                    SectionHeader("Recently viewed")
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        androidx.compose.foundation.lazy.items(recents, key = { it.id }) { w ->
                                            WallpaperCard(w, Modifier.width(120.dp)) { onOpen(w.id) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            BannerAd()
        }
    }
}

// ---------------------------------------------------------------- 4. Trending
@Composable
fun TrendingScreen(onOpen: (String) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Trending", style = MaterialTheme.typography.titleLarge) }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            FeedContent(
                feedVm("trending", WallpaperQuery(special = Special.TRENDING)), onOpen,
                Modifier.weight(1f), emptyText = emptyHint("No trending wallpapers yet."),
            )
            BannerAd()
        }
    }
}

fun emptyHint(base: String): String =
    if (ServiceLocator.repo.sourceName == "Demo")
        "$base\n\nDemo mode: connect Firebase or a Pexels key to load real wallpapers (see README)."
    else base

// ---------------------------------------------------------------- 5. Categories
private val tilePalette = listOf(
    Color(0xFF6A4CFF) to Color(0xFF2DE2C0), Color(0xFFFF5F6D) to Color(0xFFFFC371), Color(0xFF11998E) to Color(0xFF38EF7D),
    Color(0xFF4568DC) to Color(0xFFB06AB3), Color(0xFFF7971E) to Color(0xFFFFD200), Color(0xFF0F2027) to Color(0xFF2C5364),
    Color(0xFFDA22FF) to Color(0xFF9733EE), Color(0xFF00B4DB) to Color(0xFF0083B0),
)

@Composable
fun CategoriesScreen(onOpen: (String) -> Unit) {
    val extra by produceState(emptyList<WallCategory>()) { value = ServiceLocator.repo.remoteCategories() }
    val all = Categories.defaults + extra.filter { e -> Categories.defaults.none { it.slug == e.slug } }
    Scaffold(topBar = { TopAppBar(title = { Text("Categories", style = MaterialTheme.typography.titleLarge) }) }) { pad ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp), modifier = Modifier.padding(pad),
            contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(all, key = { it.slug }) { c ->
                val (a, b) = tilePalette[(c.slug.hashCode() and 0x7fffffff) % tilePalette.size]
                Box(
                    Modifier.fillMaxWidth().height(96.dp).background(Brush.linearGradient(listOf(a, b)), RoundedCornerShape(20.dp))
                        .clickable { onOpen(c.slug) }.padding(14.dp),
                    Alignment.BottomStart,
                ) {
                    Text(c.name, color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 6. Category detail
@Composable
fun CategoryScreen(slug: String, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val cat = remember(slug) { Categories.resolve(slug) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cat.name, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            val note = if (slug == "8k" || slug == "4k") "No wallpapers of this resolution yet. Only files whose real pixel size qualifies are listed." else "No wallpapers here yet."
            FeedContent(feedVm("cat:$slug", cat.query), onOpen, Modifier.weight(1f), emptyText = emptyHint(note))
            BannerAd()
        }
    }
}
