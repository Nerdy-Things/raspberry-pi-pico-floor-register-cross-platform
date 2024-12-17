package com.itkacher.raspberry.pico.registry

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Pico-Thermo-Sensor",
    ) {
        App()
    }
}