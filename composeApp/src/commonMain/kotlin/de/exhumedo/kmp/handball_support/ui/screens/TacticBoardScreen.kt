package de.exhumedo.kmp.handball_support.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import de.exhumedo.kmp.handball_support.matchconsole.TacticBoardPresenter
import de.exhumedo.kmp.handball_support.matchconsole.TacticToken
import de.exhumedo.kmp.handball_support.matchconsole.TokenType
import de.exhumedo.kmp.handball_support.ui.theme.AppTheme
import de.exhumedo.kmp.handball_support.ui.theme.DhbButton
import de.exhumedo.kmp.handball_support.ui.theme.DhbHeader
import de.exhumedo.kmp.handball_support.ui.theme.DhbRed
import de.exhumedo.kmp.handball_support.ui.theme.Dimens
import androidx.compose.foundation.Canvas as ComposeCanvas
import kotlin.math.max
import kotlin.math.min

/**
 * Handball tactical board: an accurate 2D top-down view of the court with
 * draggable player tokens and a ball. The field scales to fill the available
 * area while maintaining the standard 40 × 20 m ratio.
 *
 * Tokens are dragged with a single finger/pointer. The dragged token is
 * automatically brought to front so it renders above the others.
 */
@Composable
fun TacticBoardScreen(
    presenter: TacticBoardPresenter,
    debug: Boolean = false,
    onNavigateHome: () -> Unit,
) {
    var rotated by remember { mutableStateOf(false) }

    AppTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            DhbHeader(
                title = "Taktiktafel",
                subtitle = if (rotated) "Handball 20 × 40 m" else "Handball 40 × 20 m",
                onLogoClick = onNavigateHome,
                actions = {
                    DhbButton(onClick = { rotated = !rotated }) {
                        Text(if (rotated) "Querformat" else "Hochformat")
                    }
                    Spacer(Modifier.width(Dimens.spaceSm))
                    DhbButton(onClick = presenter::reset) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Zurücksetzen")
                    }
                    Spacer(Modifier.width(Dimens.spaceSm))
                    DhbButton(onClick = onNavigateHome) { Text("Menü") }
                },
            )

            // ── Debug panel (above the field, only with ?debug=true) ────
            if (debug) {
                val homeTokens = presenter.tokens.filter { it.type == TokenType.HOME }
                val guestTokens = presenter.tokens.filter { it.type == TokenType.GUEST }
                val homePosText = homeTokens.joinToString("\n") {
                    "${it.label}: x=${it.fieldX}, y=${it.fieldY}"
                }
                val guestPosText = guestTokens.joinToString("\n") {
                    "${it.label}: x=${it.fieldX}, y=${it.fieldY}"
                }
                Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp)
                            .background(Color.Black.copy(alpha = 0.85f))
                            .padding(6.dp),
                    ) {
                        Text("HOME", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = homePosText,
                            onValueChange = {},
                            readOnly = true,
                            textStyle = MaterialTheme.typography.labelSmall.copy(color = Color.White),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        var homeInput by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = homeInput,
                            onValueChange = { homeInput = it },
                            label = { Text("Paste Home", color = Color.White, style = MaterialTheme.typography.labelSmall) },
                            textStyle = MaterialTheme.typography.labelSmall.copy(color = Color.White),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        DhbButton(onClick = {
                            applyPositions(homeInput, presenter, TokenType.HOME)
                            homeInput = ""
                        }) {
                            Text("Apply Home", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                            .background(Color.Black.copy(alpha = 0.85f))
                            .padding(6.dp),
                    ) {
                        Text("GUEST", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = guestPosText,
                            onValueChange = {},
                            readOnly = true,
                            textStyle = MaterialTheme.typography.labelSmall.copy(color = Color.White),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        var guestInput by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = guestInput,
                            onValueChange = { guestInput = it },
                            label = { Text("Paste Guest", color = Color.White, style = MaterialTheme.typography.labelSmall) },
                            textStyle = MaterialTheme.typography.labelSmall.copy(color = Color.White),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        DhbButton(onClick = {
                            applyPositions(guestInput, presenter, TokenType.GUEST)
                            guestInput = ""
                        }) {
                            Text("Apply Guest", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val boxW = constraints.maxWidth.toFloat()
                val boxH = constraints.maxHeight.toFloat()

                // Field is 40 × 20 m. In rotated mode it is displayed as 20 × 40 m.
                val displayFieldMetresW = if (rotated) 20f else 40f
                val displayFieldMetresH = if (rotated) 40f else 20f
                val fieldAspect = displayFieldMetresW / displayFieldMetresH
                val fieldW: Float
                val fieldH: Float
                if (boxW / boxH.coerceAtLeast(1f) > fieldAspect) {
                    fieldH = boxH
                    fieldW = fieldH * fieldAspect
                } else {
                    fieldW = boxW
                    fieldH = fieldW / fieldAspect
                }
                val ox = (boxW - fieldW) / 2f   // X offset to centre field
                val oy = (boxH - fieldH) / 2f   // Y offset to centre field
                val scale = fieldW / displayFieldMetresW // pixels per metre

                val tokenRadius = (scale * 0.85f).coerceAtLeast(20f)
                val density = LocalDensity.current

                Box(Modifier.fillMaxSize()) {
                    // ── Field (fixed background) ──────────────────────────────
                    ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                        drawHandballCourt(scale = scale, ox = ox, oy = oy, rotated = rotated)
                    }

                    // ── Draggable tokens ──────────────────────────────
                    // key() is essential: bringToFront() reorders this list during
                    // onDragStart, and without stable keys the dragged chip's
                    // pointerInput is torn down mid-gesture (drag dies after the
                    // touch-slop pixels). With keys the node survives the reorder.
                    presenter.tokens.forEach { token ->
                        key(token.id) {
                            val tokenCenter = fieldToScreen(
                                fieldX = token.fieldX,
                                fieldY = token.fieldY,
                                scale = scale,
                                ox = ox,
                                oy = oy,
                                rotated = rotated,
                            )

                            TokenChip(
                                token = token,
                                centerXPx = tokenCenter.x,
                                centerYPx = tokenCenter.y,
                                radiusPx = tokenRadius,
                                density = density,
                                onDragStart = { presenter.bringToFront(token.id) },
                                onDragDelta = { dx, dy ->
                                    val deltaFieldX = if (rotated) dy / scale else dx / scale
                                    val deltaFieldY = if (rotated) -dx / scale else dy / scale
                                    presenter.moveToken(
                                        id = token.id,
                                        deltaFieldX = deltaFieldX,
                                        deltaFieldY = deltaFieldY,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TokenChip(
    token: TacticToken,
    centerXPx: Float,
    centerYPx: Float,
    radiusPx: Float,
    density: Density,
    onDragStart: () -> Unit,
    onDragDelta: (Float, Float) -> Unit,
) {
    val diameterDp = with(density) { (radiusPx * 2).toDp() }
    val topLeftXDp = with(density) { (centerXPx - radiusPx).toDp() }
    val topLeftYDp = with(density) { (centerYPx - radiusPx).toDp() }

    val bg = when (token.type) {
        TokenType.HOME -> DhbRed
        TokenType.GUEST -> GuestBlue
        TokenType.BALL -> BallYellow
        TokenType.REFEREE -> Color.Black
    }
    val textColor = if (token.type == TokenType.BALL) Color.Black else Color.White

    Box(
        modifier = Modifier
            .offset(x = topLeftXDp, y = topLeftYDp)
            .size(diameterDp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(bg)
            .border(width = 1.5.dp, color = Color.White.copy(alpha = 0.8f), shape = CircleShape)
            .pointerInput(token.id) {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragDelta(dragAmount.x, dragAmount.y)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (token.label.isNotEmpty()) {
            Text(
                text = token.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

// ── Court drawing ─────────────────────────────────────────────────────────────

private fun fieldToScreen(
    fieldX: Float,
    fieldY: Float,
    scale: Float,
    ox: Float,
    oy: Float,
    rotated: Boolean,
): Offset =
    if (rotated) {
        Offset(ox + (20f - fieldY) * scale, oy + fieldX * scale)
    } else {
        Offset(ox + fieldX * scale, oy + fieldY * scale)
    }

private fun DrawScope.drawHandballCourt(scale: Float, ox: Float, oy: Float, rotated: Boolean) {
    fun fs(m: Float) = m * scale
    fun point(x: Float, y: Float) = fieldToScreen(x, y, scale, ox, oy, rotated)

    fun drawRectField(color: Color, x: Float, y: Float, width: Float, height: Float, style: Stroke? = null) {
        val p1 = point(x, y)
        val p2 = point(x + width, y + height)
        val topLeft = Offset(min(p1.x, p2.x), min(p1.y, p2.y))
        val size = Size(max(p1.x, p2.x) - topLeft.x, max(p1.y, p2.y) - topLeft.y)
        if (style == null) {
            drawRect(color, topLeft, size)
        } else {
            drawRect(color, topLeft, size, style = style)
        }
    }

    fun drawLineField(
        color: Color,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        strokeWidth: Float,
        cap: StrokeCap = StrokeCap.Butt,
    ) {
        drawLine(color, point(startX, startY), point(endX, endY), strokeWidth, cap)
    }

    fun drawArcField(
        color: Color,
        startAngle: Float,
        sweepAngle: Float,
        topLeftX: Float,
        topLeftY: Float,
        diameter: Float,
        stroke: Stroke,
    ) {
        val radius = diameter / 2f
        val center = point(topLeftX + radius, topLeftY + radius)
        val radiusPx = fs(radius)
        drawArc(
            color = color,
            startAngle = if (rotated) startAngle + 90f else startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radiusPx, center.y - radiusPx),
            size = Size(radiusPx * 2f, radiusPx * 2f),
            style = stroke,
        )
    }

    val lw = fs(0.05f).coerceAtLeast(2f)       // standard line
    val tlw = fs(0.08f).coerceAtLeast(3f)      // thick line
    val white = Color.White
    val displayWidth = fs(if (rotated) 20f else 40f)
    val displayHeight = fs(if (rotated) 40f else 20f)

    // Green background
    drawRect(FieldGreen, Offset(ox, oy), Size(displayWidth, displayHeight))

    // Goals (outside field – drawn before clip so field overlaps the inner edge)
    drawRectField(GoalGray, -1f, 8.5f, 1f, 3f)
    drawRectField(white, -1f, 8.5f, 1f, 3f, Stroke(lw))
    drawRectField(GoalGray, 40f, 8.5f, 1f, 3f)
    drawRectField(white, 40f, 8.5f, 1f, 3f, Stroke(lw))

    // All field markings clipped to the field rect
    clipRect(ox, oy, ox + displayWidth, oy + displayHeight) {

        val dash = PathEffect.dashPathEffect(
            floatArrayOf(fs(0.4f).coerceAtLeast(4f), fs(0.25f).coerceAtLeast(3f)), 0f,
        )
        val dashedStroke = Stroke(lw, pathEffect = dash)
        val solidStroke = Stroke(lw)

        // ── 6 m goal areas ────────────────────────────────────────
        // Left  – arc centred on each post (0, 8.5) and (0, 11.5)
        drawArcField(white, 270f, 90f, -6f, 2.5f, 12f, solidStroke)
        drawArcField(white, 0f, 90f, -6f, 5.5f, 12f, solidStroke)
        drawLineField(white, 6f, 8.5f, 6f, 11.5f, lw)

        // Right – arc centred on (40, 8.5) and (40, 11.5)
        drawArcField(white, 180f, 90f, 34f, 2.5f, 12f, solidStroke)
        drawArcField(white, 90f, 90f, 34f, 5.5f, 12f, solidStroke)
        drawLineField(white, 34f, 8.5f, 34f, 11.5f, lw)

        // ── 9 m free-throw lines (dashed) ─────────────────────────
        drawArcField(white, 270f, 90f, -9f, -0.5f, 18f, dashedStroke)
        drawArcField(white, 0f, 90f, -9f, 2.5f, 18f, dashedStroke)
        drawArcField(white, 180f, 90f, 31f, -0.5f, 18f, dashedStroke)
        drawArcField(white, 90f, 90f, 31f, 2.5f, 18f, dashedStroke)

        // ── 7 m marks ─────────────────────────────────────────────
        drawLineField(white, 7f, 9.65f, 7f, 10.35f, tlw, StrokeCap.Square)
        drawLineField(white, 33f, 9.65f, 33f, 10.35f, tlw, StrokeCap.Square)

        // ── Centre line + circle ───────────────────────────────────
        drawLineField(white, 20f, 0f, 20f, 20f, lw)
        drawCircle(white, fs(3f), point(20f, 10f), style = solidStroke)

        // ── Boundary ──────────────────────────────────────────────
        drawRect(white, Offset(ox, oy), Size(displayWidth, displayHeight), style = Stroke(tlw))
    }
}

// ── Colours ───────────────────────────────────────────────────────────────────
private val FieldGreen = Color(0xFF3A7D3A)
private val GoalGray   = Color(0xFFBDBDBD)
private val GuestBlue  = Color(0xFF1565C0)
private val BallYellow = Color(0xFFFFD740)


@Preview
@Composable
private fun TacticBoardScreenPreview() {
    TacticBoardScreen(
        presenter = remember { TacticBoardPresenter() },
        onNavigateHome = {},
    )
}

/**
 * Parses pasted position text (one token per line: "LABEL: x=FLOAT, y=FLOAT")
 * and applies the coordinates to the matching tokens of [tokenType].
 */
private fun applyPositions(input: String, presenter: TacticBoardPresenter, tokenType: TokenType) {
    val regex = Regex("""(\S+):\s*x=([\d.]+),\s*y=([\d.]+)""")
    input.lineSequence().forEach { line ->
        val match = regex.find(line) ?: return@forEach
        val label = match.groupValues[1]
        val x = match.groupValues[2].toFloatOrNull() ?: return@forEach
        val y = match.groupValues[3].toFloatOrNull() ?: return@forEach
        val token = presenter.tokens.firstOrNull { it.type == tokenType && it.label == label } ?: return@forEach
        // Move token to exact position by computing delta from current
        val dx = x - token.fieldX
        val dy = y - token.fieldY
        presenter.moveToken(token.id, dx, dy)
    }
}




