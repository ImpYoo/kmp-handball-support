package de.exhumedo.kmp.handball_support.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
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
 * DHB-style page header: a white bar with the DHB logo, a screen title and
 * optional trailing actions (e.g. a logout button). Mirrors the clean, light
 * header used on dhb.de.
 */
@Composable
fun DhbHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DhbWhite)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DhbBrandMark()
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
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

/** The official DHB logo used as the header brand mark. */
@Composable
private fun DhbBrandMark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(Res.drawable.dhb_logo),
        contentDescription = "Deutscher Handballbund",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .height(40.dp)
            .width(70.dp),
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


