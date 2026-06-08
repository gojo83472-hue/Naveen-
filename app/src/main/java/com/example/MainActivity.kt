package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.data.database.UskhaDatabase
import com.example.data.repository.UskhaRepository
import com.example.ui.screens.MainDashboard
import com.example.ui.screens.FriendsListScreen
import com.example.ui.screens.TransferAndChatScreen
import com.example.ui.screens.SecurityScreen
import com.example.ui.viewmodel.UskhaViewModel

class MainActivity : ComponentActivity() {

    private lateinit var database: UskhaDatabase
    private lateinit var repository: UskhaRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Room Database
        database = Room.databaseBuilder(
            applicationContext,
            UskhaDatabase::class.java,
            "uskha_db"
        ).fallbackToDestructiveMigration().build()

        repository = UskhaRepository(database.uskhaDao())

        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(UskhaViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return UskhaViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

        setContent {
            UskhaTheme {
                val navController = rememberNavController()
                val mainViewModel: UskhaViewModel = viewModel(factory = factory)

                NavHost(navController = navController, startDestination = "dashboard") {
                    composable("dashboard") {
                        MainDashboard(
                            viewModel = mainViewModel,
                            onNavigateToFriends = { navController.navigate("friends_list") },
                            onNavigateToScan = { navController.navigate("security_scan") },
                            onNavigateToChat = { navController.navigate("chat_transfer") },
                            onNavigateToBankLimits = { navController.navigate("bank_limits") },
                            onNavigateToVideoCall = { navController.navigate("video_call") }
                        )
                    }

                    composable("friends_list") {
                        FriendsListScreen(
                            viewModel = mainViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToChat = { navController.navigate("chat_transfer") }
                        )
                    }

                    composable("chat_transfer") {
                        TransferAndChatScreen(
                            viewModel = mainViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToBankLimits = { navController.navigate("bank_limits") },
                            onNavigateToVideoCall = { navController.navigate("video_call") }
                        )
                    }

                    composable("security_scan") {
                        SecurityScreen(
                            viewModel = mainViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("bank_limits") {
                        com.example.ui.screens.BankLimitsScreen(
                            viewModel = mainViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("video_call") {
                        com.example.ui.screens.VideoCallScreen(
                            viewModel = mainViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UskhaTheme(
    darkTheme: Boolean = true, // Force beautiful dark theme with high contrast magenta/cyan elements
    content: @Composable () -> Unit
) {
    val darkColors = darkColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF00F0FF),      // High contrast neon cyan
        secondary = androidx.compose.ui.graphics.Color(0xFFEC4899),    // High contrast pink neon
        background = androidx.compose.ui.graphics.Color(0xFF0F1115),   // DatingDarkBg
        surface = androidx.compose.ui.graphics.Color(0xFF161920),      // DatingCardBg
        onPrimary = androidx.compose.ui.graphics.Color(0xFF000000),    // High contrast text inside buttons
        onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
        onBackground = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
        onSurface = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
    )

    MaterialTheme(
        colorScheme = darkColors,
        content = content
    )
}
