package com.example.recorder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.recorder.presentation.library.LibraryRoute
import com.example.recorder.presentation.library.LibraryViewModel
import com.example.recorder.presentation.recording.RecordingRoute
import com.example.recorder.presentation.recording.RecordingViewModel
import com.example.recorder.presentation.settings.SettingsRoute
import com.example.recorder.ui.theme.RecorderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RecorderApp() }
    }
}

@Composable
private enum class RecorderDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Record("record", "Record", Icons.Default.Mic),
    Library("library", "Library", Icons.Default.LibraryMusic),
    Settings("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun RecorderApp(navController: NavHostController = rememberNavController()) {
    val destinations = RecorderDestination.values().toList()
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route ?: RecorderDestination.Record.route
    val currentDestination = destinations.firstOrNull { it.route == currentRoute } ?: RecorderDestination.Record
    RecorderTheme {
        Scaffold(
            topBar = { TopBar(currentDestination.label) },
            bottomBar = {
                NavigationBar {
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            label = { Text(destination.label) },
                            icon = { Icon(destination.icon, contentDescription = destination.label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "record",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("record") {
                    val viewModel: RecordingViewModel = hiltViewModel()
                    RecordingRoute(viewModel)
                }
                composable("library") {
                    val viewModel: LibraryViewModel = hiltViewModel()
                    LibraryRoute(viewModel)
                }
                composable("settings") {
                    SettingsRoute()
                }
            }
        }
    }
}

@Composable
private fun TopBar(title: String) {
    TopAppBar(title = { Text(text = title, style = MaterialTheme.typography.titleLarge) })
}
