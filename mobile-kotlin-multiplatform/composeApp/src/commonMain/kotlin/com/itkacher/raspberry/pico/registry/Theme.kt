package com.itkacher.raspberry.pico.registry

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable

// Define dark mode colors
private val DarkColorPalette = darkColors(
    primary = Color(0xFFBB86FC), // Light purple
    primaryVariant = Color(0xFF3700B3),
    secondary = Color(0xFF03DAC6), // Teal
    background = Color(0xFF121212), // Dark background
    surface = Color(0xFF1F1B24), // Darker surface
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun ThermoSensorAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = DarkColorPalette,
        content = content
    )
}
