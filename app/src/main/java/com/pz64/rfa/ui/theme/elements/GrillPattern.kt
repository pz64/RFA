package com.pz64.rfa.ui.theme.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Applies a repeating "grill" or "perforated" pattern to the background.
 * Perfect for that industrial hardware/teenage engineering look.
 *
 * @param color The color of the holes/dots.
 * @param dotSize The size of each individual hole.
 * @param spacing The distance between the centers of the holes.
 */
fun Modifier.grillBackground(
    color: Color = Color.Black.copy(alpha = 0.15f),
    dotSize: Dp = 2.dp,
    spacing: Dp = 4.dp
) = this.drawWithCache {
    val dotSizePx = dotSize.toPx()
    val spacingPx = spacing.toPx()

    onDrawBehind {
        val columns = (size.width / spacingPx).toInt() + 1
        val rows = (size.height / spacingPx).toInt() + 1

        for (i in 0 until columns) {
            for (j in 0 until rows) {
                drawCircle(
                    color = color,
                    radius = dotSizePx / 2f,
                    center = Offset(i * spacingPx, j * spacingPx)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GrillPatternPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(Color(0xFFE0E0E0)) // Light gray hardware surface
                .grillBackground(
                    color = Color.Black.copy(alpha = 0.2f),
                    dotSize = 3.dp,
                    spacing = 6.dp
                )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GrillOnButtonPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .size(width = 100.dp, height = 40.dp)
                .background(Color(0xFFD4D4D4))
                .grillBackground()
        )
    }
}
