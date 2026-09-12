package de.exhumedo.kmp.handball_support.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.exhumedo.kmp.handball_support.coaching.RefereeCoachingPresenter
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.Criterion
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.DefectGroup
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.RootCause
import de.exhumedo.kmp.handball_support.referee_coaching.domain.model.ScoringConfig
import de.exhumedo.kmp.handball_support.ui.theme.DhbButton
import de.exhumedo.kmp.handball_support.ui.theme.DhbRed
import de.exhumedo.kmp.handball_support.ui.theme.Dimens

/**
 * Reconstructs a [RefereeCoachingPresenter] from a saved [CoachingReportResponseDto].
 * Starts from the default catalog and applies the reported root-cause counts so the
 * sheet can be rendered (read-only or continued) without losing the saved state.
 */
fun restoreCoachingFromReport(presenter: RefereeCoachingPresenter, report: de.exhumedo.kmp.handball_support.client.CoachingReportResponseDto) {
    presenter.reset()
    report.rows.forEach { row ->
        row.defectGroups.forEach { group ->
            group.selectedRootCauses.forEach { cause ->
                val count = cause.count
                if (count > 0) {
                    repeat(count) { presenter.select(row.criterionId, group.groupId, cause.rootCauseId) }
                } else if (count < 0) {
                    repeat(-count) { presenter.deselect(row.criterionId, group.groupId, cause.rootCauseId) }
                }
            }
        }
    }
}

/**
 * Emits the referee-coaching sheet (HVNB "Beobachterbogen") into a [LazyListScope]:
 * a score summary, the two sections (A "Spielregeln", B "Persönlicher Eindruck")
 * and an expandable card per criterion.
 *
 * Provided as a `LazyListScope` extension so it can share a single scroll
 * container with other content (e.g. the stopwatch/scoreboard on the coaching
 * session screen) instead of nesting scrollables.
 *
 * @param expanded observable per-criterion expansion state (e.g. `mutableStateMapOf`).
 * @param onSelected invoked after a root cause is selected (incremented), e.g. to log history.
 * @param onDeselected invoked after a root cause is deselected (decremented), e.g. to log history.
 */
fun LazyListScope.coachingSheet(
    presenter: RefereeCoachingPresenter,
    expanded: MutableMap<String, Boolean>,
    readOnly: Boolean = false,
    onSelected: (criterionId: String, groupId: String, rootCauseId: String) -> Unit = { _, _, _ -> },
    onDeselected: (criterionId: String, groupId: String, rootCauseId: String) -> Unit = { _, _, _ -> },
) {
    item(key = "coaching-summary") {
        ScoreSummaryCard(
            totalScore = presenter.totalScore,
            maxTotalScore = presenter.maxTotalScore,
            adjustedCriteria = presenter.adjustedCriteriaCount,
            criteriaCount = presenter.criteria.size,
        )
    }

    item(key = "coaching-section-a") { SectionHeader("A · Spielregeln") }
    items(presenter.rulesOfTheGame, key = { it.id }) { criterion ->
        CriterionCard(
            criterion = criterion,
            expanded = expanded[criterion.id] == true,
            readOnly = readOnly,
            onToggleExpand = { expanded[criterion.id] = expanded[criterion.id] != true },
            onSelect = { groupId, causeId ->
                presenter.select(criterion.id, groupId, causeId)
                onSelected(criterion.id, groupId, causeId)
            },
            onDeselect = { groupId, causeId ->
                presenter.deselect(criterion.id, groupId, causeId)
                onDeselected(criterion.id, groupId, causeId)
            },
        )
    }

    item(key = "coaching-section-b") { SectionHeader("B · Persönlicher Eindruck") }
    items(presenter.personalImpression, key = { it.id }) { criterion ->
        CriterionCard(
            criterion = criterion,
            expanded = expanded[criterion.id] == true,
            readOnly = readOnly,
            onToggleExpand = { expanded[criterion.id] = expanded[criterion.id] != true },
            onSelect = { groupId, causeId ->
                presenter.select(criterion.id, groupId, causeId)
                onSelected(criterion.id, groupId, causeId)
            },
            onDeselect = { groupId, causeId ->
                presenter.deselect(criterion.id, groupId, causeId)
                onDeselected(criterion.id, groupId, causeId)
            },
        )
    }
}

@Composable
private fun ScoreSummaryCard(
    totalScore: Int,
    maxTotalScore: Int,
    adjustedCriteria: Int,
    criteriaCount: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cardCorner),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = Dimens.cardElevation,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.spaceLg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Gesamtpunktzahl",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (adjustedCriteria == 0) {
                        "Keine Anpassungen · $criteriaCount Kriterien"
                    } else {
                        "$adjustedCriteria von $criteriaCount Kriterien angepasst"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "$totalScore / $maxTotalScore",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Dimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = Dimens.accentBarWidth, height = Dimens.accentBarHeight)
                .clip(RoundedCornerShape(Dimens.accentBarWidth))
                .background(DhbRed),
        )
        Spacer(Modifier.width(Dimens.spaceMd))
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun CriterionCard(
    criterion: Criterion,
    expanded: Boolean,
    readOnly: Boolean,
    onToggleExpand: () -> Unit,
    onSelect: (groupId: String, rootCauseId: String) -> Unit,
    onDeselect: (groupId: String, rootCauseId: String) -> Unit,
) {
    val markedCount = criterion.defectGroups.sumOf { group -> group.rootCauses.count { it.count != 0 } }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cardCorner),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (criterion.score != ScoringConfig.DEFAULT_SCORE) 2.dp else 1.dp,
            color = if (criterion.score != ScoringConfig.DEFAULT_SCORE) scoreColor(criterion.score)
            else MaterialTheme.colorScheme.outlineVariant,
        ),
        shadowElevation = if (expanded) Dimens.cardElevationRaised else Dimens.cardElevation,
    ) {
        Column(modifier = Modifier.padding(Dimens.spaceLg)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = criterion.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val groupsText = criterion.defectGroups.joinToString(", ") { it.name }
                    Text(
                        text = "($groupsText)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                    Text(
                        text = if (markedCount == 0) "Tippen zum Aufklappen" else "$markedCount Fehler markiert",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ScoreBadge(score = criterion.score)
                Spacer(Modifier.width(Dimens.spaceSm))
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = if (expanded) "Zuklappen" else "Aufklappen",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (expanded) {
                Spacer(Modifier.height(Dimens.spaceMd))
                criterion.defectGroups.forEachIndexed { index, group ->
                    if (index > 0) Spacer(Modifier.height(Dimens.spaceMd))
                    DefectGroupBlock(
                        group = group,
                        readOnly = readOnly,
                        onSelect = { causeId -> onSelect(group.id, causeId) },
                        onDeselect = { causeId -> onDeselect(group.id, causeId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DefectGroupBlock(
    group: DefectGroup,
    readOnly: Boolean,
    onSelect: (rootCauseId: String) -> Unit,
    onDeselect: (rootCauseId: String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = group.name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(Dimens.spaceSm))
        group.rootCauses.forEach { cause ->
            RootCauseStepper(
                cause = cause,
                readOnly = readOnly,
                onIncrement = { onSelect(cause.id) },
                onDecrement = { onDeselect(cause.id) },
            )
            Spacer(Modifier.height(Dimens.spaceXs))
        }
    }
}

@Composable
private fun RootCauseStepper(
    cause: RootCause,
    readOnly: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    val active = cause.count != 0
    // Positive counts deduct points (red); negative counts grant a bonus (green).
    val countColor = when {
        cause.count > 0 -> DhbRed
        cause.count < 0 -> ScoreGood
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = cause.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!readOnly) {
            DhbButton(onClick = onDecrement) { Text("-") }
        }
        Box(
            modifier = Modifier.widthIn(min = 32.dp).padding(horizontal = Dimens.spaceSm),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = cause.count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = countColor,
            )
        }
        if (!readOnly) {
            DhbButton(onClick = onIncrement) { Text("+") }
        }
    }
}

@Composable
private fun ScoreBadge(score: Int) {
    val color = scoreColor(score)
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(2.dp, color),
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
    }
}

/** Traffic-light colour for a criterion score: green (good), amber (mid), red (low). */
private fun scoreColor(score: Int): Color = when {
    score >= ScoringConfig.DEFAULT_SCORE -> ScoreGood
    score >= 3 -> ScoreMid
    else -> DhbRed
}

private val ScoreGood = Color(0xFF2E7D32)
private val ScoreMid = Color(0xFFB8860B)


