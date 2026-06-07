package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import com.example.ui.screens.UskhaMainApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.UskhaViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // SAFELY INSTANTIATE VIEWMODEL
    val viewModel = ViewModelProvider(this)[UskhaViewModel::class.java]

    setContent {
      val isDarkTheme by viewModel.isDarkTheme.collectAsState()
      MyApplicationTheme(darkTheme = isDarkTheme) {
        UskhaMainApp(viewModel = viewModel)
      }
    }
  }
}
