package de.exhumedo.kmp.handball_support

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import de.exhumedo.kmp.handball_support.navigation.captureLaunchDeepLink

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Must run before App() so bindToBrowserNavigation captures `/` as its base path.
    captureLaunchDeepLink()
    ComposeViewport {
        App()
    }
}
