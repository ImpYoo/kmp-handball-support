package de.exhumedo.kmp.handball_support.config

/**
 * Compile-time feature flags controlling which application modules are available.
 *
 * The values are provided by variant-specific source sets so the compiler can strip
 * unused code paths and Compose routes from a build.
 */
expect object AppVariant {
    val showRating: Boolean
    val showRefereeCoaching: Boolean
    val showCoachingSheet: Boolean
    val showMatchConsole: Boolean
    val showRoster: Boolean
    val showMatchSetup: Boolean
    val showDrawingPad: Boolean
    val showTacticBoard: Boolean
}
