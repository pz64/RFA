package com.pz64.rfa.ui.main

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.pz64.rfa.data.ChannelData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class MainScreenViewModel : ViewModel() {

    private val channels = mutableStateListOf<ChannelData>()
    fun handleAction(action: Action) {
        when (action) {
            is Action.AddChannel -> {
                addChannel(action.channelData)
            }

            is Action.RemoveChannel -> {
                removeChannel(action.channelData)
            }
        }
    }

    private fun removeChannel(channelData: ChannelData) {
        channels.remove(channelData)
        _state.update { if (channels.isEmpty()) State.NoChannels else State.WithChannels(channels) }
    }

    private fun addChannel(channelData: ChannelData) {
        channels.add(channelData)
        _state.update { State.WithChannels(channels) }
    }

    private val _state = MutableStateFlow<State>(State.NoChannels)
    val state: StateFlow<State> = _state

    sealed interface State {
        object NoChannels : State
        data class WithChannels(val channels: List<ChannelData>) : State
    }

    sealed class Action {
        class AddChannel(val channelData: ChannelData) : Action()
        class RemoveChannel(val channelData: ChannelData) : Action()
    }
}