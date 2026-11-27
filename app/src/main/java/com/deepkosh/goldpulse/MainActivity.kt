package com.deepkosh.goldpulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.deepkosh.goldpulse.ui.HomeRoute
import com.deepkosh.goldpulse.ui.theme.GoldPulseTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            GoldPulseTheme {
                HomeRoute()
            }
        }
    }
}
