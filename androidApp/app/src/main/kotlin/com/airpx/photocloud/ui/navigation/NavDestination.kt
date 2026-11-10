package com.airpx.photocloud.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val titleResId: Int? = null, val icon: ImageVector? = null) {
    object Library : Screen("library", icon = Icons.Default.PhotoLibrary)
    object Collections : Screen("collections", icon = Icons.Default.CollectionsBookmark)
    object Search : Screen("search", icon = Icons.Default.Search)
    object Settings : Screen("settings")
    object ServerConnection : Screen("server_connection")
    object AutoBackup : Screen("auto_backup")
    object Albums : Screen("albums")
    object Favorites : Screen("favorites")
    object PhotoDetail : Screen("photo_detail/{photoId}") {
        fun createRoute(photoId: String) = "photo_detail/$photoId"
    }
}
