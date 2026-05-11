package de.exhumedo.kmp.handball_support.domain.rating.model

sealed class Evaluator {
    abstract val type: EvaluatorType

    data class RefereeTeam(val refereePair: RefereePair) : Evaluator() {
        override val type: EvaluatorType = EvaluatorType.REFEREE_TEAM
    }

    data class Delegate(val assignment: RoleAssignment) : Evaluator() {
        override val type: EvaluatorType = EvaluatorType.DELEGATE
    }
}
