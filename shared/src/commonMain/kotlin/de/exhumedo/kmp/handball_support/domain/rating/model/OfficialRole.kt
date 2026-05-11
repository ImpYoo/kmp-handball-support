package de.exhumedo.kmp.handball_support.domain.rating.model
sealed class OfficialRole {
    object FirstReferee : OfficialRole()
    object SecondReferee : OfficialRole()
    object Delegate : OfficialRole()
    object TimeKeeper : OfficialRole()
    object ScoreKeeper : OfficialRole()
}
