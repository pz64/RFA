package com.pz64.rfa.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface NavDestination {
    @Serializable
    data object Main : NavDestination
    
    @Serializable
    data object Settings : NavDestination
}
