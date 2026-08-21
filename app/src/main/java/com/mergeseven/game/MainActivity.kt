package com.mergeseven.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mergeseven.game.app.AppNavGraph
import com.mergeseven.game.ui.theme.MergeSevenTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single activity entry point for Merge Seven.
 * Uses Jetpack Compose for all UI rendering.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MergeSevenTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    AppNavGraph()
                }
            }
        }
    }
}
