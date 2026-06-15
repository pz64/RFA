package com.pz64.rfa.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pz64.rfa.R
import com.pz64.rfa.data.ChannelData
import com.pz64.rfa.ui.theme.RFATheme
import com.pz64.rfa.ui.theme.elements.RFButton
import com.pz64.rfa.ui.theme.elements.RFText
import com.pz64.rfa.ui.theme.elements.customScrollbar
import com.pz64.rfa.ui.theme.elements.grillBackground

@Composable
fun MainScreenRoute(
    viewModel: MainScreenViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    MainScreen(
        state,
        onAddStationClick = { channelData ->
            viewModel.handleAction(MainScreenViewModel.Action.AddChannel(channelData))
        }
    )
}


@Composable
fun MainScreen(
    state: MainScreenViewModel.State = MainScreenViewModel.State.NoChannels,
    onAddStationClick: (channelData: ChannelData) -> Unit = {}
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        bottomBar = {
            BottomAppBar(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceBright)
                    .grillBackground(MaterialTheme.colorScheme.outlineVariant),
                containerColor = Color.Transparent,
                actions = {
                    // Put your navigation icons or other actions here
                },
                floatingActionButton = {
                    RFButton(
                        onClick = {
                            onAddStationClick(
                                ChannelData(
                                    "Channel name 1",
                                    "FM",
                                    175000f
                                )
                            )
                        },
                        icon = {
                            Icon(
                                // Use the custom EditAudio icon we created or a resource
                                painter = painterResource(R.drawable.edit_audio_24px),
                                contentDescription = "Add Station",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        text = { RFText("Add Station") }
                    )
                },
                tonalElevation = 8.dp
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is MainScreenViewModel.State.NoChannels -> {
                    RFText("No Channels")
                }

                is MainScreenViewModel.State.WithChannels -> {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .customScrollbar(listState)
                    ) {
                        items(state.channels, key = { it.id }) {
                            ChannelItem(it)
                        }
                    }
                }
            }

        }
    }
}


@Composable
@Preview
fun MainScreenPreviewNoChannels() {
    RFATheme {
        MainScreen()
    }
}

@Composable
@Preview
fun MainScreenPreviewWithChannels() {
    RFATheme {
        MainScreen(
            state = MainScreenViewModel.State.WithChannels(
                channels = listOf(
                    ChannelData(
                        "Channel name 1",
                        "FM",
                        175000f
                    ),
                    ChannelData(
                        "Channel name 2",
                        "AM",
                        175000f
                    ),
                    ChannelData(
                        "Channel name 3",
                        "AM",
                        175000f
                    )
                )
            )
        )
    }
}