package de.exhumedo.kmp.handball_support.referee_coaching.domain

import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.Criterion
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.DefectGroup
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.RootCause
import de.exhumedo.kmp.handball_support.referee_coaching.domain.scoring.CriterionScoringService
import kotlin.test.Test
import kotlin.test.assertEquals

class CriterionScoringServiceTest {

    private val service = CriterionScoringService()

    private val groupId = "group-1"
    private val rootCauseId = "cause-1"

    private fun newCriterion(score: Int = 6, count: Int = 0) = Criterion(
        id = "A1",
        name = "Game Understanding",
        score = score,
        defectGroups = listOf(
            DefectGroup(
                id = groupId,
                name = "Advantage Rule",
                rootCauses = listOf(
                    RootCause(id = rootCauseId, name = "Whistle against game flow", count = count),
                ),
            ),
        ),
    )

    private fun increment(criterion: Criterion, times: Int): Criterion {
        var current = criterion
        repeat(times) {
            current = service.incrementRootCause(current, groupId, rootCauseId)
        }
        return current
    }

    private fun decrement(criterion: Criterion, times: Int): Criterion {
        var current = criterion
        repeat(times) {
            current = service.decrementRootCause(current, groupId, rootCauseId)
        }
        return current
    }

    private fun count(criterion: Criterion): Int =
        criterion.defectGroups.first().rootCauses.first().count

    // --- Increment / threshold behaviour ---

    @Test
    fun noPointDeductedBeforeFirstThreshold() {
        val result = increment(newCriterion(), times = 2)
        assertEquals(6, result.score)
        assertEquals(2, count(result))
    }

    @Test
    fun firstPointDeductedAfterThreeIncrements() {
        val result = increment(newCriterion(), times = 3)
        assertEquals(5, result.score)
    }

    @Test
    fun secondPointDeductedAfterEightIncrements() {
        val result = increment(newCriterion(), times = 8)
        assertEquals(4, result.score)
    }

    @Test
    fun thirdPointDeductedAfterFifteenIncrements() {
        val result = increment(newCriterion(), times = 15)
        assertEquals(3, result.score)
    }

    @Test
    fun scoreUnchangedBetweenThresholds() {
        // counts 3..7 all map to penalty 1
        for (c in 3..7) {
            assertEquals(5, increment(newCriterion(), times = c).score, "count=$c")
        }
        // counts 8..14 all map to penalty 2
        for (c in 8..14) {
            assertEquals(4, increment(newCriterion(), times = c).score, "count=$c")
        }
    }

    // --- Decrement behaviour ---

    @Test
    fun decrementRestoresPointWhenThresholdCrossedBackward() {
        val deducted = increment(newCriterion(), times = 3) // score 5, count 3
        val restored = decrement(deducted, times = 1) // count 2 -> penalty 0
        assertEquals(6, restored.score)
        assertEquals(2, count(restored))
    }

    @Test
    fun decrementDoesNotRestoreWhileStillAboveThreshold() {
        val deducted = increment(newCriterion(), times = 8) // score 4, count 8
        val stepBack = decrement(deducted, times = 1) // count 7 -> penalty 1
        assertEquals(5, stepBack.score)
    }

    @Test
    fun fullDecrementRestoresOriginalScore() {
        val deducted = increment(newCriterion(), times = 15) // score 3
        val restored = decrement(deducted, times = 15)
        assertEquals(6, restored.score)
        assertEquals(0, count(restored))
    }

    // --- Symmetric negative behaviour (bonus points) ---

    @Test
    fun countCanGoNegative() {
        val result = decrement(newCriterion(), times = 5)
        assertEquals(-5, count(result))
    }

    @Test
    fun firstBonusAddedAfterThreeDecrements() {
        // Symmetric to the deduction side: -3 -> +1 point.
        val result = decrement(newCriterion(), times = 3)
        assertEquals(7, result.score)
    }

    @Test
    fun secondBonusAddedAfterEightDecrements() {
        // -8 -> +2 points.
        val result = decrement(newCriterion(), times = 8)
        assertEquals(8, result.score)
    }

    @Test
    fun negativeAndPositiveMappingsAreMirrorImages() {
        for (n in 0..15) {
            val deducted = increment(newCriterion(), times = n).score
            val bonused = decrement(newCriterion(), times = n).score
            // bonus = default + (default - deducted), i.e. symmetric around the default score.
            assertEquals(6 - deducted, bonused - 6, "n=$n")
        }
    }

    @Test
    fun incrementUndoesADecrementSymmetrically() {
        val bonused = decrement(newCriterion(), times = 3) // score 7, count -3
        val restored = increment(bonused, times = 1) // count -2 -> penalty 0
        assertEquals(6, restored.score)
        assertEquals(-2, count(restored))
    }

    // --- Boundary behaviour ---
    @Test
    fun scoreNeverExceedsMax() {
        // Start at max already; decrementing must not push above max.
        val result = decrement(newCriterion(score = 9), times = 3)
        assertEquals(9, result.score)
    }

    @Test
    fun scoreNeverDropsBelowZero() {
        // Start low; many increments must clamp at 0.
        val result = increment(newCriterion(score = 1), times = 15) // penalty 3 -> clamps at 0
        assertEquals(0, result.score)
    }

    // --- Targeting behaviour ---

    @Test
    fun unknownGroupOrCauseLeavesCriterionUnchanged() {
        val base = newCriterion()
        assertEquals(base, service.incrementRootCause(base, "missing-group", rootCauseId))
        assertEquals(base, service.incrementRootCause(base, groupId, "missing-cause"))
    }

    @Test
    fun penaltiesAccumulateAcrossMultipleRootCauses() {
        val criterion = Criterion(
            id = "A1",
            name = "Game Understanding",
            defectGroups = listOf(
                DefectGroup(
                    id = "g1",
                    name = "Group 1",
                    rootCauses = listOf(RootCause(id = "c1", name = "C1", count = 3)), // penalty 1
                ),
                DefectGroup(
                    id = "g2",
                    name = "Group 2",
                    rootCauses = listOf(RootCause(id = "c2", name = "C2", count = 0)),
                ),
            ),
        )
        // Incrementing c2 to 3 should deduct one more point on top of the existing penalty.
        var result = criterion
        repeat(3) { result = service.incrementRootCause(result, "g2", "c2") }
        // Note: score field starts at default 6; only the c2 change is applied via delta.
        assertEquals(5, result.score)
    }
}

