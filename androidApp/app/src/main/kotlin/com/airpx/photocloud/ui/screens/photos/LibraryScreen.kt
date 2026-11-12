package com.airpx.photocloud.ui.screens.photos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.airpx.photocloud.data.mediastore.MediaStoreScanner
import com.airpx.photocloud.domain.model.LibraryMediaFilter
import com.airpx.photocloud.domain.model.LibrarySortOrder
import com.airpx.photocloud.domain.model.MediaType
import com.airpx.photocloud.domain.model.PhotoItem

@Composable
fun LibraryScreen(
    hasPhotoPermission: Boolean,
    onRequestPermission: () -> Unit,
    onPhotoClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var sortOrder by remember { mutableStateOf(LibrarySortOrder.CAPTURE_DATE) }
    var mediaFilter by remember { mutableStateOf(LibraryMediaFilter.ALL) }
    var photos by remember { mutableStateOf<List<PhotoItem>>(emptyList()) }
    var showFilterMenu by remember { mutableStateOf(false) }

    LaunchedEffect(hasPhotoPermission, sortOrder, mediaFilter) {
        if (hasPhotoPermission) {
            val scanner = MediaStoreScanner(context)
            photos = scanner.scanPhotos(sortOrder, mediaFilter)
        } else {
            photos = emptyList()
        }
    }

    if (!hasPhotoPermission) {
        PhotoAccessPrompt(
            onRequestPermission = onRequestPermission,
            modifier = modifier
        )
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            // Sort & Filter bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                IconButton(
                    onClick = { showFilterMenu = true },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Sortieren und filtern"
                    )
                }
                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Aufnahmedatum") },
                        onClick = {
                            sortOrder = LibrarySortOrder.CAPTURE_DATE
                            showFilterMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Hinzugefügt am") },
                        onClick = {
                            sortOrder = LibrarySortOrder.DATE_ADDED
                            showFilterMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Alle Medien") },
                        onClick = {
                            mediaFilter = LibraryMediaFilter.ALL
                            showFilterMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Nur Fotos") },
                        onClick = {
                            mediaFilter = LibraryMediaFilter.PHOTOS
                            showFilterMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Nur Videos") },
                        onClick = {
                            mediaFilter = LibraryMediaFilter.VIDEOS
                            showFilterMenu = false
                        }
                    )
                }
            }

            if (photos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Keine Fotos",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Für diese Auswahl sind keine Medien verfügbar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 96.dp),
                    contentPadding = PaddingValues(1.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(photos, key = { it.id }) { photo ->
                        PhotoGridTile(
                            photo = photo,
                            onClick = { onPhotoClick(photo.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoGridTile(
    photo: PhotoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photo.contentUri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (photo.mediaType == MediaType.VIDEO) {
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = "Video",
                tint = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(20.dp)
            )
        }
    }
}

@Composable
fun PhotoAccessPrompt(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "AirPx benötigt Zugriff auf deine Mediathek, damit du deine Fotos hier ansehen kannst. Auto-Backup bleibt ausgeschaltet, bis du es aktivierst.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRequestPermission) {
                Text(text = "Zugriff erlauben")
            }
        }
    }
}
