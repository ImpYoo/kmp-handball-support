package de.exhumedo.kmp.handball_support.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Centers page content and caps its width on large screens for comfortable line
 * lengths, while giving generous, consistent gutters. Children are stacked in a
 * column with even vertical spacing.
 */
@Composable
fun PageContainer(
    modifier: Modifier = Modifier,
    horizontalPadding: androidx.compose.ui.unit.Dp = Dimens.spaceLg,
    verticalPadding: androidx.compose.ui.unit.Dp = Dimens.spaceLg,
    spacing: androidx.compose.ui.unit.Dp = Dimens.spaceLg,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = Dimens.contentMaxWidth)
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content,
        )
    }
}

