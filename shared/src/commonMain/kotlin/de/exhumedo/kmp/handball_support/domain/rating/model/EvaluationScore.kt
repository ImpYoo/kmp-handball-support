package de.exhumedo.kmp.handball_support.domain.rating.model

data class EvaluationScore(
    val appearance: Score,
    val influence: Score,
    val teamwork: Score,
) {
    fun toScore(): Int = appearance.value + influence.value + teamwork.value
}
