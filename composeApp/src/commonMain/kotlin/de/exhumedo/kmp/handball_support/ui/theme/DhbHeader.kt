package de.exhumedo.kmp.handball_support.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import handball_support.composeapp.generated.resources.Res
import handball_support.composeapp.generated.resources.dhb_logo
import org.jetbrains.compose.resources.painterResource

/**
 * DHB-style page header: an elevated white bar with the DHB logo, a screen title
 * and optional trailing actions (e.g. a logout button). The content is centered
 * and width-capped to align with the page body on large screens.
 */
@Composable
fun DhbHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onLogoClick: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DhbWhite,
        shadowElevation = 3.dp,
    ) {
        Box(contentAlignment = Alignment.TopCenter) {
            Row(
                modifier = Modifier
                    .widthIn(max = Dimens.contentMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceMd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DhbBrandMark(onLogoClick = onLogoClick)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = Dimens.spaceMd),
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleLarge,
                        color = DhbBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = DhbBlack.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                actions()
            }
        }
    }
}

/** The official DHB logo used as the header brand mark. Clickable when [onLogoClick] is provided. */
@Composable
private fun DhbBrandMark(
    modifier: Modifier = Modifier,
    onLogoClick: (() -> Unit)? = null,
) {
    val baseModifier = modifier
        .height(40.dp)
        .width(70.dp)
    val effectiveModifier = if (onLogoClick != null) {
        baseModifier.clickable(onClick = onLogoClick)
    } else {
        baseModifier
    }
    Image(
        painter = painterResource(Res.drawable.dhb_logo),
        contentDescription = "Deutscher Handballbund",
        contentScale = ContentScale.Fit,
        modifier = effectiveModifier,
    )
}

/** A thin black accent rule used to separate header from content. */
@Composable
fun DhbAccentRule(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(DhbBlack),
    )
}


