package com.example.recorder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import com.example.recorder.presentation.library.LibraryRoute
import com.example.recorder.presentation.library.LibraryViewModel
import com.example.recorder.presentation.recording.RecordingRoute
import com.example.recorder.presentation.recording.RecordingViewModel
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
fun RecorderApp(navController: NavHostController = rememberNavController()) {
    RecorderTheme {
        Scaffold(topBar = { TopBar() }) { innerPadding ->
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
            }
        }
    }
}

@Composable
private fun TopBar() {
    TopAppBar(title = { Text(text = "Recorder", style = MaterialTheme.typography.titleLarge) })
}
