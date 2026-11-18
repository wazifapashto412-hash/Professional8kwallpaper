package com.infinity.wallpapers.navigation

import android.net.Uri
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.infinity.wallpapers.ServiceLocator
import com.infinity.wallpapers.ads.AdsManager
import com.infinity.wallpapers.ui.screens.*
import com.infinity.wallpapers.utils.findActivity

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Home", Icons.Filled.Home),
    Tab("trending", "Trending", Icons.Filled.TrendingUp),
    Tab("categories", "Categories", Icons.Filled.Category),
    Tab("favorites", "Favorites", Icons.Filled.Favorite),
    Tab("settings", "Settings", Icons.Filled.Settings),
)

private fun NavHostController.openWallpaper(id: String) = navigate("detail/${Uri.encode(id)}")

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val ctx = LocalContext.current
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (tabs.any { it.route == route }) {
                NavigationBar {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = route == t.route,
                            onClick = {
                                nav.navigate(t.route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(t.icon, t.label) },
                            label = { Text(t.label) },
                        )
                    }
                }
            }
        },
    ) { pad ->
        NavHost(nav, startDestination = "splash", modifier = Modifier.padding(pad).consumeWindowInsets(pad)) {
            composable("splash") {
                SplashScreen { dest -> nav.navigate(dest) { popUpTo("splash") { inclusive = true } } }
            }
            composable("onboarding") {
                OnboardingScreen {
                    ServiceLocator.settings.onboarded = true
                    nav.navigate("home") { popUpTo("onboarding") { inclusive = true } }
                }
            }
            composable("offline") {
                OfflineScreen(
                    onRetry = { nav.navigate("home") { popUpTo("offline") { inclusive = true } } },
                    onDownloads = { nav.navigate("downloads") },
                )
            }
            composable("home") {
                HomeScreen(onOpen = nav::openWallpaper, onSearch = { nav.navigate("search") }, onSeeAll = { nav.navigate("category/$it") })
            }
            composable("trending") { TrendingScreen(onOpen = nav::openWallpaper) }
            composable("categories") { CategoriesScreen(onOpen = { nav.navigate("category/${Uri.encode(it)}") }) }
            composable("category/{slug}") { e ->
                CategoryScreen(e.arguments?.getString("slug") ?: "latest", onBack = { nav.popBackStack() }, onOpen = nav::openWallpaper)
            }
            composable("detail/{id}") { e ->
                val id = e.arguments?.getString("id") ?: return@composable
                DetailScreen(
                    id,
                    onBack = {
                        nav.popBackStack()
                        ctx.findActivity()?.let { AdsManager.onDetailClosed(it) } // natural navigation break
                    },
                    onPreview = { nav.navigate("preview/${Uri.encode(id)}") },
                )
            }
            composable("preview/{id}") { e ->
                PreviewScreen(e.arguments?.getString("id") ?: "", onBack = { nav.popBackStack() })
            }
            composable("favorites") { FavoritesScreen(onOpen = nav::openWallpaper, onDownloads = { nav.navigate("downloads") }) }
            composable("downloads") { DownloadsScreen(onBack = { nav.popBackStack() }, onOpen = nav::openWallpaper) }
            composable("search") { SearchScreen(onBack = { nav.popBackStack() }, onOpen = nav::openWallpaper) }
            composable("settings") { SettingsScreen(onPrivacy = { nav.navigate("privacy") }, onAbout = { nav.navigate("about") }) }
            composable("privacy") { PrivacyScreen { nav.popBackStack() } }
            composable("about") { AboutScreen { nav.popBackStack() } }
        }
    }
}
