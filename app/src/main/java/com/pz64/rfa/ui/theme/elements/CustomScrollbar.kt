package com.pz64.rfa.ui.theme.elements

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A custom scrollbar modifier for LazyColumn that matches the RFA hardware aesthetic.
 */
fun Modifier.customScrollbar(
    state: LazyListState,
    width: Dp = 4.dp,
    thumbColor: Color = Color(0xFFFF9800), // RFA Orange
    trackColor: Color = Color.Transparent
): Modifier = composed {
    val targetAlpha = if (state.isScrollInProgress) 1f else 0f
    val duration = if (state.isScrollInProgress) 150 else 500

    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = duration),
        label = "scrollbar_alpha"
    )

    drawWithContent {
        drawContent()

        val layoutInfo = state.layoutInfo
        val visibleItemsInfo = layoutInfo.visibleItemsInfo

        if (visibleItemsInfo.isNotEmpty() && alpha > 0.0f) {
            val totalItemsCount = layoutInfo.totalItemsCount
            val visibleItemsCount = visibleItemsInfo.size

            if (visibleItemsCount < totalItemsCount) {
                val scrollbarWidthPx = width.toPx()

                // Track
                if (trackColor != Color.Transparent) {
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(size.width - scrollbarWidthPx, 0f),
                        size = Size(scrollbarWidthPx, size.height),
                        alpha = alpha
                    )
                }

                // Thumb calculation
                val firstVisibleItem = visibleItemsInfo.first()

                val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset

                // Simple estimate of total content height
                val averageItemHeight =
                    visibleItemsInfo.sumOf { it.size }.toFloat() / visibleItemsCount
                val totalHeight = averageItemHeight * totalItemsCount

                val scrollOffset =
                    (firstVisibleItem.index * averageItemHeight) - firstVisibleItem.offset

                val thumbHeight = (viewportHeight.toFloat() / totalHeight) * viewportHeight
                val thumbMinHeight = 40.dp.toPx()
                val finalThumbHeight = thumbHeight.coerceAtLeast(thumbMinHeight)

                val scrollPercent = scrollOffset / (totalHeight - viewportHeight)
                val thumbOffset = scrollPercent * (viewportHeight - finalThumbHeight)

                drawRoundRect(
                    color = thumbColor,
                    topLeft = Offset(size.width - scrollbarWidthPx - 2.dp.toPx(), thumbOffset),
                    size = Size(scrollbarWidthPx, finalThumbHeight),
                    cornerRadius = CornerRadius(scrollbarWidthPx / 2, scrollbarWidthPx / 2),
                    alpha = alpha
                )
            }
        }
    }
}
