package com.airpx.photocloud.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.airpx.photocloud.data.PhotoCloudApi
import com.airpx.photocloud.data.PhotoSyncScheduler
import com.airpx.photocloud.data.SyncSettings
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ServerConnectionScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings = remember { SyncSettings(context) }
    val scope = rememberCoroutineScope()
    var serverUrl by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var accountEmail by remember { mutableStateOf("") }
    var loggedIn by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        serverUrl = settings.server()
        loggedIn = settings.token() != null
        if (loggedIn) accountEmail = settings.accountEmail()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Server",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text("Server-URL") },
            placeholder = { Text("https://airpx.example.com") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                scope.launch {
                    busy = true
                    status = try {
                        val valid = withContext(Dispatchers.IO) { PhotoCloudApi(serverUrl, "").checkServer() }
                        if (valid) { settings.saveServer(serverUrl); "Server erreichbar" } else "Server nicht erreichbar"
                    } catch (_: Exception) { "Server nicht erreichbar. URL und Netzwerk prüfen." }
                    busy = false
                }
            },
            enabled = serverUrl.isNotBlank() && !busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Server prüfen")
        }
        if (status.isNotBlank()) Text(
            status,
            color = if (status.contains("fehlgeschlagen") || status.contains("nicht erreichbar") || status.contains("nicht korrekt")) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        if (loggedIn) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Angemeldet als", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(accountEmail.ifBlank { "Angemeldet" })
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = {
                scope.launch {
                    settings.logout()
                    PhotoSyncScheduler.setAutomatic(context, false)
                    loggedIn = false
                    accountEmail = ""
                    status = "Abgemeldet"
                }
            }) { Text("Abmelden") }
        } else {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Anmelden",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-Mail-Adresse") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Passwort") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
            Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                scope.launch {
                    busy = true
                    status = ""
                    status = try {
                        val token = withContext(Dispatchers.IO) { PhotoCloudApi(serverUrl, "").login(email, password) }
                        settings.saveServer(serverUrl)
                        settings.saveToken(token)
                        settings.saveAccountEmail(email)
                        accountEmail = email.trim()
                        loggedIn = true
                        "Erfolgreich angemeldet"
                    } catch (e: Exception) {
                        if (e is IOException) "Server nicht erreichbar. URL und Netzwerk prüfen."
                        else e.message ?: "Anmeldung fehlgeschlagen. Bitte Anmeldedaten prüfen."
                    }
                    password = ""
                    busy = false
                }
            },
            enabled = email.isNotBlank() && password.isNotBlank() && serverUrl.isNotBlank() && !busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Anmelden")
        }
        }
    }
}
