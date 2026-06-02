package de.exhumedo.kmp.handball_support.navigation

/**
 * Returns the deep-link encoded in the launch URL (e.g. `phases`, `phases/12345`,
 * `phases/12345/matches/678`, `login`, optionally with `?day=…&month=…&year=…`). Empty on
 * non-web targets.
 */
expect fun initialDeepLink(): String
