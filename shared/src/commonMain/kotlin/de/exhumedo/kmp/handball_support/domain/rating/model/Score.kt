package de.exhumedo.kmp.handball_support.domain.rating.model

data class Score(val value: Int) {
    init {
        require(value in MIN_VALUE..MAX_VALUE) {
            "Score value must be between $MIN_VALUE and $MAX_VALUE, got $value."
        }
    }

    companion object {
        const val MIN_VALUE = 1
        const val MAX_VALUE = 10
    }
}
