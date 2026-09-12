package de.exhumedo.kmp.handball_support.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.exhumedo.kmp.handball_support.client.CoachingReportResponseDto
import de.exhumedo.kmp.handball_support.ui.theme.Dimens

@Composable
fun CoachingReportCard(report: CoachingReportResponseDto) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cardCorner),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = Dimens.cardElevation,
    ) {
        Column(modifier = Modifier.padding(Dimens.spaceLg)) {
            Text(
                text = "Bewertung",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(Dimens.spaceMd))
            Text(
                text = "Gesamtpunktzahl: ${report.totalScore} / ${report.maxTotalScore} (${report.percentage}%)",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(Dimens.spaceSm))
            report.rows.forEach { row ->
                if (row.deductionPoints != 0 || row.defectGroups.any { it.selectedRootCauses.isNotEmpty() }) {
                    Text(
                        text = "${row.criterionName}: ${row.score}/${row.maxScore} (−${row.deductionPoints})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    row.defectGroups.forEach { group ->
                        group.selectedRootCauses.forEach { cause ->
                            Text(
                                text = "  • ${group.groupName}: ${cause.rootCauseName} ×${cause.count}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Spacer(Modifier.height(Dimens.spaceSm))
                }
            }
        }
    }
}
