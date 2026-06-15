package com.pz64.rfa.ui.theme.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pz64.rfa.ui.theme.RFATheme
import com.pz64.rfa.ui.theme.orangeAccent

@Composable
fun RFButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showOrangeStroke: Boolean = true,
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit
) {
    Box(
        modifier = modifier.width(IntrinsicSize.Max)
    ) {

        val orangeStrokePadding = if (showOrangeStroke) 6.dp else 0.dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.onSecondary)
                .grillBackground(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    dotSize = 1.dp,
                    spacing = 4.dp
                )
                .clickable(onClick = onClick)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                .padding(
                    top = 8.dp + orangeStrokePadding,
                    bottom = 8.dp,
                    start = 12.dp,
                    end = 12.dp
                )
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.padding(vertical = 4.dp)) {
                icon()
            }

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Box {
                text()
            }
        }

        if (showOrangeStroke) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(orangeStrokePadding)
                    .background(orangeAccent, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun RFButtonPreview() {
    RFATheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RFButton(
                onClick = {},
                icon = {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = { RFText("Add Station") }
            )

            RFButton(
                onClick = {},
                showOrangeStroke = false,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = { RFText("LOOP") }
            )
        }
    }
}
