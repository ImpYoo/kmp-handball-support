package de.exhumedo.kmp.handball_support.vote

sealed interface UiEvent {
    data class Error(val message: String) : UiEvent
    data class Info(val message: String) : UiEvent
    data object SessionExpired : UiEvent
}

