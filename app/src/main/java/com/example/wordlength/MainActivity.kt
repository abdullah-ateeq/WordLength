package com.example.wordlength

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.wordlength.ui.navigation.WordLengthApp
import com.example.wordlength.ui.theme.WordLengthTheme
import com.example.wordlength.ui.viewmodel.WordLengthViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: WordLengthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsState()

            WordLengthTheme(
                appTheme = settings.theme,
                dynamicColor = settings.dynamicColor
            ) {
                WordLengthApp(viewModel = viewModel)
            }
        }
    }
}
