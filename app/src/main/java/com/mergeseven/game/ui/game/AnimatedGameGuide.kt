package com.mergeseven.game.ui.game

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.mergeseven.game.core.audio.GameFont
import com.mergeseven.game.ui.components.GoldButton
import com.mergeseven.game.ui.theme.GameColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Native offline animation: no video download, GIF decoder or frame work while hidden. */
@Composable
internal fun AnimatedGameGuide(page: Int, reduceMotion: Boolean, onPage: (Int) -> Unit, onFinish: () -> Unit) {
    val step = page.coerceIn(0, 2)
    val titles = listOf("Place your first piece", "Rotate for a better fit", "Make your first merge")
    val explanations = listOf(
        "Tap a piece in the tray, then tap an empty board space. You can also drag it. A preview shows whether it fits.",
        "Select a piece and tap Rotate. Turn it until every tile fits an empty space. Invalid placements cost nothing.",
        "Connect three or more equal numbers. Three 2 tiles merge into one 4. A new match can start another chain!"
    )
    AlertDialog(
        onDismissRequest = onFinish,
        containerColor = GameColors.WoodDark,
        title = { Text(titles[step], style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(Modifier.heightIn(max = (LocalConfiguration.current.screenHeightDp * .55f).dp)
                .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("How to play · ${step + 1} of 3", color = GameColors.CoinGold)
                GuideIllustration(step, reduceMotion, Modifier.fillMaxWidth().height(160.dp))
                Text(explanations[step], style = MaterialTheme.typography.bodyLarge)
                if (reduceMotion) Text("Reduced motion: the finished move is shown.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            GoldButton(if (step == 2) "Let's play" else "Next", onClick = {
                if (step == 2) onFinish() else onPage(step + 1)
            })
        },
        dismissButton = {
            Row {
                if (step > 0) TextButton({ onPage(step - 1) }) { Text("Back") }
                TextButton(onFinish) { Text("Skip guide") }
            }
        }
    )
}

@Composable
private fun GuideIllustration(step: Int, reduceMotion: Boolean, modifier: Modifier) {
    val progress = remember { Animatable(0f) }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(step, reduceMotion, owner) {
        if (reduceMotion) progress.snapTo(1f)
        else owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                progress.snapTo(0f)
                progress.animateTo(1f, tween(2800, easing = LinearEasing))
                delay(700)
            }
        }
    }
    val context = LocalContext.current
    val numberPaint = remember(context) { Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = GameFont.bold(context); color = android.graphics.Color.rgb(24, 24, 24)
        textAlign = Paint.Align.CENTER
    } }
    val hex = remember { Path() }
    val descriptions = listOf("A tray piece moves into an empty board space", "A two-tile piece turns sixty degrees",
        "Three connected 2 tiles join to become a 4 tile")
    Canvas(modifier.semantics { contentDescription = descriptions[step] }) {
        // Read the clock in drawing, without recomposing or measuring the dialog on every frame.
        val phase = progress.value
        val radius = minOf(size.width / 7f, size.height / 5.4f)
        val center = Offset(size.width * .52f, size.height * .48f)
        fun tile(at: Offset, value: Int?, color: Color, scale: Float = 1f) {
            hex.reset()
            for (i in 0..5) {
                val angle = i * PI / 3
                val x = at.x + cos(angle).toFloat() * radius * scale
                val y = at.y + sin(angle).toFloat() * radius * scale
                if (i == 0) hex.moveTo(x, y) else hex.lineTo(x, y)
            }
            hex.close()
            drawPath(hex, color)
            drawPath(hex, GameColors.CoinGold.copy(alpha = .5f), style = Stroke(2.dp.toPx()))
            if (value != null) {
                numberPaint.textSize = radius * .8f * scale
                drawContext.canvas.nativeCanvas.drawText(value.toString(), at.x,
                    at.y - (numberPaint.ascent() + numberPaint.descent()) / 2, numberPaint)
            }
        }
        val blue = Color(0xFF43B7E8)
        val green = Color(0xFF53D477)
        when (step) {
            0 -> {
                val target = Offset(size.width * .66f, size.height * .35f)
                val start = Offset(size.width * .25f, size.height * .74f)
                val move = ((phase - .2f) / .55f).coerceIn(0f, 1f)
                val at = start + (target - start) * move
                tile(target, null, Color.White.copy(alpha = .12f))
                tile(target + Offset(radius * 1.5f, radius * .866f), null, Color.White.copy(alpha = .12f))
                tile(at, 2, blue)
                tile(at + Offset(radius * 1.5f, radius * .866f), 4, green)
                if (phase < .8f) {
                    val touch = at + Offset(-radius * .25f, radius * .5f)
                    drawCircle(Color.White, radius * .19f, touch)
                    drawCircle(GameColors.CoinGold, radius * .35f, touch, style = Stroke(2.dp.toPx()))
                }
            }
            1 -> {
                val turn = ((phase - .15f) / .65f).coerceIn(0f, 1f) * (PI / 3).toFloat()
                tile(center, 2, blue)
                val angle = (PI / 6).toFloat() + turn
                tile(center + Offset(cos(angle) * radius * 1.74f, sin(angle) * radius * 1.74f), 4, green)
                drawArc(GameColors.CoinGold, -130f, 210f, false,
                    center - Offset(radius * 2.45f, radius * 2.45f),
                    Size(radius * 4.9f, radius * 4.9f), style = Stroke(3.dp.toPx()))
            }
            else -> {
                val join = ((phase - .3f) / .4f).coerceIn(0f, 1f)
                if (join < 1f) {
                    tile(center - Offset(radius * 1.5f, radius * .866f) * (1f - join), 2, blue)
                    tile(center + Offset(radius * 1.5f, radius * .866f) * (1f - join), 2, blue)
                    tile(center, 2, blue)
                } else {
                    drawCircle(GameColors.CoinGold.copy(alpha = .7f * (1f - phase)),
                        radius * (1.3f + phase), center, style = Stroke(3.dp.toPx()))
                    tile(center, 4, green, 1f + .12f * sin((phase - .7f) / .3f * PI).toFloat())
                }
            }
        }
    }
}
