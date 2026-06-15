package com.pz64.rfa.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pz64.rfa.data.models.ChannelData
import com.pz64.rfa.ui.theme.RFATheme
import com.pz64.rfa.ui.theme.SevenSegmentFont
import com.pz64.rfa.ui.theme.elements.RFText
import com.pz64.rfa.ui.theme.elements.grillBackground
import com.pz64.rfa.ui.theme.lcdBG
import com.pz64.rfa.ui.theme.orangeAccent
import java.util.Locale

@Composable
fun ChannelItem(
    channelData: ChannelData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .height(IntrinsicSize.Min)
    ) {
        val orangeStrokeWidth = 6.dp

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
                .border(1.dp, MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(4.dp))
                .padding(start = 12.dp + orangeStrokeWidth, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                RFText(
                    text = channelData.channelName,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                RFText(
                    text = channelData.modulation,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            )

            Surface(
                shape = MaterialTheme.shapes.small, color = lcdBG
            ) {
                Text(
                    text = String.format(Locale.ROOT, "%.3f MHz", channelData.frequency / 1000f),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onTertiaryFixedVariant,
                    fontFamily = SevenSegmentFont, modifier = Modifier.padding(8.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(orangeStrokeWidth)
                .background(
                    orangeAccent,
                    RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
                )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun ChannelItemPreview() {
    RFATheme {
        Column(modifier = Modifier.padding(16.dp)) {
            ChannelItem(
                channelData = ChannelData(
                    channelName = "Radio One",
                    modulation = "FM",
                    frequency = 101100f
                )
            )
        }
    }
}
