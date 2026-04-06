package de.exhumedo.kmp.handball_support.domain.rating.model

import de.exhumedo.kmp.handball_support.domain.rating.exception.DomainException
import kotlin.jvm.JvmInline

/**
 * Federation score on a fixed scale from 1 to 10.
 *
 * @property value The validated numeric score value.
 */
@JvmInline
value class Score(
    val value: Int,
) {
    init {
        if (value !in MIN_VALUE..MAX_VALUE) {
            throw DomainException.InvalidScoreRange(value, MIN_VALUE, MAX_VALUE)
        }
    }

    /**
     * Returns the raw score number for display and logging.
     */
    override fun toString(): String = value.toString()

    companion object {
        /**
         * The lowest permitted score.
         */
        const val MIN_VALUE: Int = 1

        /**
         * The highest permitted score.
         */
        const val MAX_VALUE: Int = 10
    }
}
