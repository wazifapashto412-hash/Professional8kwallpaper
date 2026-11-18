package com.infinity.wallpapers.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class WallpaperItem(
    val id: Int,
    val title: String,
    val category: String
)

@Composable
fun Screen1(
    onWallpaperSelected: (Int) -> Unit = {}
) {
    val wallpapers = remember {
        listOf(
            WallpaperItem(1, "Nature 8K", "Nature"),
            WallpaperItem(2, "Mountain 8K", "Nature"),
            WallpaperItem(3, "Abstract 8K", "Abstract"),
            WallpaperItem(4, "Dark 8K", "Dark"),
            WallpaperItem(5, "Space 8K", "Space"),
            WallpaperItem(6, "Beautiful 8K", "Nature"),
            WallpaperItem(7, "Galaxy 8K", "Space"),
            WallpaperItem(8, "Minimal 8K", "Abstract")
        )
    }

    var searchText by remember { mutableStateOf("") }
    var showFavorites by remember { mutableStateOf(false) }
    val favoriteIds = remember { mutableStateOf(setOf<Int>()) }

    val filteredWallpapers = wallpapers.filter { wallpaper ->
        val matchesSearch =
            wallpaper.title.contains(searchText, ignoreCase = true) ||
            wallpaper.category.contains(searchText, ignoreCase = true)

        val matchesFavorites =
            !showFavorites || favoriteIds.value.contains(wallpaper.id)

        matchesSearch && matchesFavorites
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Infinity 8K Wallpapers",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Premium Wallpapers for Your Screen",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchText,
                onValueChange = { newValue ->
                    searchText = newValue
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search Wallpapers") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = { showFavorites = false }
                ) {
                    Text("All Wallpapers")
                }

                TextButton(
                    onClick = { showFavorites = true }
                ) {
                    Text("Favorites")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredWallpapers.isEmpty()) {
                Text(
                    text = "No wallpapers found.",
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = filteredWallpapers,
                        key = { wallpaper -> wallpaper.id }
                    ) { wallpaper ->

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 4.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = wallpaper.title,
                                    style = MaterialTheme.typography.titleLarge
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = wallpaper.category,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement =
                                        Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            onWallpaperSelected(wallpaper.id)
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Select")
                                    }

                                    TextButton(
                                        onClick = {
                                            val currentFavorites =
                                                favoriteIds.value

                                            favoriteIds.value =
                                                if (currentFavorites.contains(
                                                        wallpaper.id
                                                    )
                                                ) {
                                                    currentFavorites -
                                                        wallpaper.id
                                                } else {
                                                    currentFavorites +
                                                        wallpaper.id
                                                }
                                        }
                                    ) {
                                        Text(
                                            if (favoriteIds.value.contains(
                                                    wallpaper.id
                                                )
                                            ) {
                                                "♥ Saved"
                                            } else {
                                                "♡ Favorite"
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
}
