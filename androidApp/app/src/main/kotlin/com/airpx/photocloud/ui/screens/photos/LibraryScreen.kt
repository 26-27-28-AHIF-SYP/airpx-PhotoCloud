package com.airpx.photocloud.ui.screens.photos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.airpx.photocloud.PhotoPermissionStatus
import com.airpx.photocloud.data.mediastore.MediaStoreScanner
import com.airpx.photocloud.domain.model.LibraryMediaFilter
import com.airpx.photocloud.domain.model.LibrarySortOrder
import com.airpx.photocloud.domain.model.MediaType
import com.airpx.photocloud.domain.model.PhotoItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    permissionStatus: PhotoPermissionStatus,
    userPickedPhotos: List<PhotoItem>,
    onRequestPermission: () -> Unit,
    onSelectMorePhotos: () -> Unit,
    onPickPhotos: () -> Unit,
    onPhotoClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var sortOrder by remember { mutableStateOf(LibrarySortOrder.CAPTURE_DATE) }
    var mediaFilter by remember { mutableStateOf(LibraryMediaFilter.ALL) }
    var photos by remember { mutableStateOf<List<PhotoItem>>(emptyList()) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var showAddPhotosSheet by remember { mutableStateOf(false) }

    val bottomSheetState = rememberModalBottomSheetState()

    LaunchedEffect(permissionStatus, sortOrder, mediaFilter, userPickedPhotos) {
        if (permissionStatus != PhotoPermissionStatus.DENIED) {
            val scanner = MediaStoreScanner(context)
            val scanned = scanner.scanPhotos(sortOrder, mediaFilter)
            val filteredUserPicked = when (mediaFilter) {
                LibraryMediaFilter.ALL -> userPickedPhotos
                LibraryMediaFilter.PHOTOS -> userPickedPhotos.filter { it.mediaType == MediaType.PHOTO }
                LibraryMediaFilter.VIDEOS -> userPickedPhotos.filter { it.mediaType == MediaType.VIDEO }
            }
            val combined = (scanned + filteredUserPicked).distinctBy { it.contentUri }
            photos = when (sortOrder) {
                LibrarySortOrder.CAPTURE_DATE -> combined.sortedByDescending { it.creationTimestamp }
                LibrarySortOrder.DATE_ADDED -> combined.sortedByDescending { it.addedTimestamp }
            }
        } else {
            photos = userPickedPhotos
        }
    }

    if (permissionStatus == PhotoPermissionStatus.DENIED && photos.isEmpty()) {
        PhotoAccessPrompt(
            onRequestPermission = onRequestPermission,
            onPickPhotos = onPickPhotos,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Partial photo access banner
                if (permissionStatus == PhotoPermissionStatus.PARTIAL) {
                    PartialAccessBanner(
                        onSelectMorePhotos = onSelectMorePhotos,
                        onPickPhotos = onPickPhotos
                    )
                }

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
                                text = "Tippe unten auf 'Fotos hinzufügen', um Medien auszuwählen.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                if (permissionStatus == PhotoPermissionStatus.PARTIAL) {
                                    showAddPhotosSheet = true
                                } else {
                                    onPickPhotos()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Fotos hinzufügen")
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 96.dp),
                        contentPadding = PaddingValues(bottom = 80.dp, start = 1.dp, end = 1.dp, top = 1.dp),
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

            // Floating Action Button to add more photos
            ExtendedFloatingActionButton(
                onClick = {
                    if (permissionStatus == PhotoPermissionStatus.PARTIAL) {
                        showAddPhotosSheet = true
                    } else if (permissionStatus == PhotoPermissionStatus.DENIED) {
                        onRequestPermission()
                    } else {
                        onPickPhotos()
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Fotos hinzufügen"
                    )
                },
                text = { Text("Fotos hinzufügen") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }

        // Add photos sheet for partial access options
        if (showAddPhotosSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddPhotosSheet = false },
                sheetState = bottomSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Text(
                        text = "Fotos hinzufügen",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                    ListItem(
                        headlineContent = { Text("Weitere Fotos freigeben") },
                        supportingContent = { Text("System-Auswahl zur App-Freigabe erweitern") },
                        leadingContent = {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null)
                        },
                        modifier = Modifier.clickable {
                            showAddPhotosSheet = false
                            onSelectMorePhotos()
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Fotos über Dateiauswahl importieren") },
                        supportingContent = { Text("Gezielt Medien aus der Fotogalerie wählen") },
                        leadingContent = {
                            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null)
                        },
                        modifier = Modifier.clickable {
                            showAddPhotosSheet = false
                            onPickPhotos()
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun PartialAccessBanner(
    onSelectMorePhotos: () -> Unit,
    onPickPhotos: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Eingeschränkter Foto-Zugriff",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Du hast Zugriff auf ausgewählte Fotos gewährt. Du kannst jederzeit weitere Fotos freigeben oder importieren.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onSelectMorePhotos) {
                    Text("Weitere Fotos freigeben")
                }
                OutlinedButton(onClick = onPickPhotos) {
                    Text("Importieren")
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
    onPickPhotos: () -> Unit,
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
                text = "AirPx benötigt Zugriff auf deine Mediathek, damit du deine Fotos hier ansehen kannst. Du kannst auch gezielt Fotos über die Fotogalerie auswählen.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onRequestPermission) {
                    Text(text = "Zugriff erlauben")
                }
                OutlinedButton(onClick = onPickPhotos) {
                    Text(text = "Fotos auswählen")
                }
            }
        }
    }
}
