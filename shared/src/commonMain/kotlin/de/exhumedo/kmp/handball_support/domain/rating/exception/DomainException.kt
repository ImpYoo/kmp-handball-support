package de.exhumedo.kmp.handball_support.domain.rating.exception

sealed class DomainException(message: String) : Exception(message) {

    class CommentTooLong(actualLength: Int, maxLength: Int) :
        DomainException("Comment length ${actualLength} exceeds maximum allowed length of ${maxLength} characters.")

    class DuplicateGameEvaluation(gameId: String, evaluatorType: String) :
        DomainException("An evaluation for game ${gameId} and evaluator type ${evaluatorType} already exists.")

    class InvalidRoleAssignment(expectedRole: String, actualRole: String, personId: String) :
        DomainException("Expected role ${expectedRole} but got ${actualRole} for person ${personId}.")
}
