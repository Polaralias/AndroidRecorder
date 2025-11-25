package com.example.recorder.presentation.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result -> viewModel.onSignInResult(result.data) }

    SettingsScreen(
        state = uiState,
        onToggleAutoBackup = viewModel::onAutoBackupChanged,
        onBackupAll = viewModel::onBackupAll,
        onLaunchSignIn = { launcher.launch(viewModel.signInIntent) },
        onApiKeyChanged = viewModel::onApiKeyChanged,
        onSaveApiKey = viewModel::onSaveApiKey,
        onTestApiKey = viewModel::onTestApiKey,
        onClearApiKey = viewModel::onClearApiKey
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onToggleAutoBackup: (Boolean) -> Unit,
    onBackupAll: () -> Unit,
    onLaunchSignIn: () -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onSaveApiKey: () -> Unit,
    onTestApiKey: () -> Unit,
    onClearApiKey: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Settings, contentDescription = null)
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Google Drive backup", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Automatically save new recordings to your Drive app folder.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = state.autoBackupEnabled, onCheckedChange = onToggleAutoBackup)
                }

                Button(
                    onClick = onLaunchSignIn,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isBackingUp
                ) {
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null)
                    Text(
                        text = state.accountEmail?.let { "Signed in as $it" } ?: "Sign in with Google",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                HorizontalDivider()

                Button(
                    onClick = onBackupAll,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isBackingUp
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Text(text = "Back up pending recordings", modifier = Modifier.padding(start = 8.dp))
                }

                Text(
                    text = state.statusMessage.ifBlank { state.lastBackupLabel },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = state.lastBackupLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Online transcription (Google Cloud)", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Bring your own Google Cloud Speech-to-Text key to run transcriptions online.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("1. Create a project in the Google Cloud console.")
                    Text("2. Enable the Speech-to-Text API for that project.")
                    Text("3. Create an API key and paste it below.")
                }
                OutlinedTextField(
                    value = state.apiKeyInput,
                    onValueChange = onApiKeyChanged,
                    label = { Text("Google Cloud API key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onSaveApiKey, enabled = state.apiKeyInput.isNotBlank()) {
                        Text("Save key")
                    }
                    Button(
                        onClick = onTestApiKey,
                        enabled = state.apiKeyInput.isNotBlank() && !state.isTestingKey
                    ) {
                        Text(if (state.isTestingKey) "Testing…" else "Test connection")
                    }
                    TextButton(onClick = onClearApiKey, enabled = state.hasStoredApiKey) {
                        Text("Remove")
                    }
                }
                val helperMessage = when {
                    state.keyTestMessage.isNotBlank() -> state.keyTestMessage
                    state.hasStoredApiKey -> "Key saved securely on this device."
                    else -> "Your key stays private and is never bundled with the app."
                }
                Text(
                    text = helperMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
