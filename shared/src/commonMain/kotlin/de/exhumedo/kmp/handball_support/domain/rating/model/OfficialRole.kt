package de.exhumedo.kmp.handball_support.domain.rating.model

/**
 * Role held by a person within the context of a specific game.
 *
 * A person is not inherently a referee or table official in the abstract
 * domain model. The role only exists when that person is assigned to one game.
 */
sealed class OfficialRole {

    /**
     * Display name used in messages and logs.
     */
    abstract val displayName: String

    /**
     * The primary referee role for a game.
     */
    data object FirstReferee : OfficialRole() {
        override val displayName: String = "FirstReferee"
    }

    /**
     * The secondary referee role for a game.
     */
    data object SecondReferee : OfficialRole() {
        override val displayName: String = "SecondReferee"
    }

    /**
     * The timekeeper role at the officials' table.
     */
    data object TimeKeeper : OfficialRole() {
        override val displayName: String = "TimeKeeper"
    }

    /**
     * The scorekeeper role at the officials' table.
     */
    data object ScoreKeeper : OfficialRole() {
        override val displayName: String = "ScoreKeeper"
    }

    /**
     * The optional delegate role associated with the table official team.
     */
    data object Delegate : OfficialRole() {
        override val displayName: String = "Delegate"
    }

    companion object {
        /**
         * The roles that make up a referee pair.
         */
        val refereeRoles: Set<OfficialRole> = setOf(FirstReferee, SecondReferee)

        /**
         * The roles that make up the evaluated table official team.
         */
        val tableRoles: Set<OfficialRole> = setOf(TimeKeeper, ScoreKeeper, Delegate)
    }
}
