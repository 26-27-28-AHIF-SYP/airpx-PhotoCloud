package com.airpx.photocloud

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.airpx.photocloud.data.mediastore.MediaStoreScanner
import com.airpx.photocloud.domain.model.PhotoItem
import com.airpx.photocloud.ui.navigation.Screen
import com.airpx.photocloud.ui.screens.albums.AlbumsScreen
import com.airpx.photocloud.ui.screens.albums.CollectionsScreen
import com.airpx.photocloud.ui.screens.photos.FavoritesScreen
import com.airpx.photocloud.ui.screens.photos.LibraryScreen
import com.airpx.photocloud.ui.screens.photos.PhotoDetailScreen
import com.airpx.photocloud.ui.screens.photos.SearchScreen
import com.airpx.photocloud.ui.screens.settings.ServerConnectionScreen
import com.airpx.photocloud.ui.screens.settings.SettingsScreen
import com.airpx.photocloud.ui.screens.sync.AutoBackupScreen
import com.airpx.photocloud.ui.theme.AirPxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AirPxTheme {
                AirPxAppShell()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirPxAppShell() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Library) }
    var previousMainTab by remember { mutableStateOf<Screen>(Screen.Library) }
    var selectedPhotoId by remember { mutableStateOf<String?>(null) }
    var loadedPhotos by remember { mutableStateOf<List<PhotoItem>>(emptyList()) }

    // Check media permission
    var hasPhotoPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.READ_MEDIA_IMAGES] == true ||
                permissions[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true ||
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        hasPhotoPermission = granted
    }

    fun requestPermissions() {
        val permissionsToRequest = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES
            )
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissionsToRequest)
    }

    val bottomNavItems = listOf(
        Screen.Library to "Mediathek",
        Screen.Collections to "Sammlungen",
        Screen.Search to "Suchen"
    )

    if (selectedPhotoId != null) {
        PhotoDetailScreen(
            photos = loadedPhotos,
            initialPhotoId = selectedPhotoId!!,
            onClose = { selectedPhotoId = null },
            onDeletePhoto = { deleted ->
                loadedPhotos = loadedPhotos.filter { it.id != deleted.id }
                selectedPhotoId = null
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when (currentScreen) {
                                Screen.Library -> "Mediathek"
                                Screen.Collections -> "Sammlungen"
                                Screen.Search -> "Suchen"
                                Screen.Settings -> "Einstellungen"
                                Screen.ServerConnection -> "Serververbindung"
                                Screen.AutoBackup -> "Auto-Backup"
                                Screen.Albums -> "Alben"
                                Screen.Favorites -> "Favoriten"
                                else -> "AirPx"
                            }
                        )
                    },
                    navigationIcon = {
                        when (currentScreen) {
                            Screen.ServerConnection, Screen.AutoBackup -> {
                                IconButton(onClick = { currentScreen = Screen.Settings }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Zurück"
                                    )
                                }
                            }
                            Screen.Albums, Screen.Favorites -> {
                                IconButton(onClick = { currentScreen = Screen.Collections }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Zurück"
                                    )
                                }
                            }
                            Screen.Settings -> {
                                IconButton(onClick = { currentScreen = previousMainTab }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Schließen"
                                    )
                                }
                            }
                            else -> { }
                        }
                    },
                    actions = {
                        if (currentScreen in listOf(Screen.Library, Screen.Collections, Screen.Search)) {
                            IconButton(onClick = {
                                previousMainTab = currentScreen
                                currentScreen = Screen.Settings
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Einstellungen"
                                )
                            }
                        } else if (currentScreen == Screen.Settings) {
                            TextButton(onClick = { currentScreen = previousMainTab }) {
                                Text("Fertig")
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (currentScreen in bottomNavItems.map { it.first }) {
                    NavigationBar {
                        bottomNavItems.forEach { (screen, label) ->
                            NavigationBarItem(
                                selected = currentScreen == screen,
                                onClick = { currentScreen = screen },
                                icon = {
                                    screen.icon?.let { icon ->
                                        Icon(imageVector = icon, contentDescription = label)
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            val modifier = Modifier.padding(innerPadding)
            when (currentScreen) {
                Screen.Library -> LibraryScreen(
                    hasPhotoPermission = hasPhotoPermission,
                    onRequestPermission = { requestPermissions() },
                    onPhotoClick = { photoId ->
                        val scanner = MediaStoreScanner(context)
                        loadedPhotos = scanner.scanPhotos()
                        selectedPhotoId = photoId
                    },
                    modifier = modifier
                )
                Screen.Collections -> CollectionsScreen(
                    onNavigateToAlbums = { currentScreen = Screen.Albums },
                    onNavigateToFavorites = { currentScreen = Screen.Favorites },
                    modifier = modifier
                )
                Screen.Search -> SearchScreen(modifier = modifier)
                Screen.Settings -> SettingsScreen(
                    onNavigateToServerConnection = { currentScreen = Screen.ServerConnection },
                    onNavigateToAutoBackup = { currentScreen = Screen.AutoBackup },
                    modifier = modifier
                )
                Screen.ServerConnection -> ServerConnectionScreen(modifier = modifier)
                Screen.AutoBackup -> AutoBackupScreen(
                    hasPhotoPermission = hasPhotoPermission,
                    onRequestPermission = { requestPermissions() },
                    modifier = modifier
                )
                Screen.Albums -> AlbumsScreen(modifier = modifier)
                Screen.Favorites -> FavoritesScreen(modifier = modifier)
                else -> LibraryScreen(
                    hasPhotoPermission = hasPhotoPermission,
                    onRequestPermission = { requestPermissions() },
                    onPhotoClick = { photoId ->
                        val scanner = MediaStoreScanner(context)
                        loadedPhotos = scanner.scanPhotos()
                        selectedPhotoId = photoId
                    },
                    modifier = modifier
                )
            }
        }
    }
}
