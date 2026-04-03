package de.exhumedo.kmp.handball_support

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "handball_support",
    ) {
        App()
    }
}