package de.exhumedo.kmp.handball_support.domain.model

sealed interface DomainError {
    val code: String
    val message: String
}

data object PhaseNotFoundError : DomainError {
    override val code: String = "PHASE_NOT_FOUND"
    override val message: String = "phase not found"
}

data object MatchNotFoundError : DomainError {
    override val code: String = "MATCH_NOT_FOUND"
    override val message: String = "match not found"
}

data object ForbiddenVoterError : DomainError {
    override val code: String = "FORBIDDEN_VOTER"
    override val message: String = "voterId has no permission for this match"
}

data object InvalidRatingError : DomainError {
    override val code: String = "INVALID_RATING"
    override val message: String = "ratings must be within range 1..5"
}

data object VoteAlreadyExistsError : DomainError {
    override val code: String = "VOTE_ALREADY_EXISTS"
    override val message: String = "vote already exists for this voter and match"
}

data object CommentTooLongError : DomainError {
    override val code: String = "COMMENT_TOO_LONG"
    override val message: String = "comment exceeds max length"
}

data object MissingDelegateError : DomainError {
    override val code: String = "MISSING_DELEGATE"
    override val message: String = "match has no delegate assigned; delegate vote is not allowed"
}

