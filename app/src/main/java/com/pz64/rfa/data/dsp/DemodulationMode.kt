package com.pz64.rfa.data.dsp

/**
 * <h1>RF Analyzer - Demodulation Tab</h1>
 *
 * Module:      DemodulationTab.kt
 * Description: A Tab in the Control Drawer. Contains all settings related to
 * signal demodulation.
 *
 * @author Dennis Mantz
 *
 * Copyright (C) 2025 Dennis Mantz
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */


enum class DemodulationMode(
    val displayName: String,
    val minChannelWidth: Int,
    val maxChannelWidth: Int,
    val defaultChannelWidth: Int,
    val tuneStepDistance: Int
) {
    OFF("OFF", 0, 50000, 0, 0),  //dummy channel width values; equals largest channel width (=WFM)
    AM("AM", 3000, 20000, 10000, 1000),
    NFM("FM (narrow)", 3000, 15000, 10000, 1000),
    WFM("FM (wide)", 30000, 150000, 100000, 100000),
    LSB("LSB", 1500, 5000, 2800, 100),
    USB("USB", 1500, 5000, 2800, 100),
    CW("CW", 100, 600, 200, 50),
    //DIGITAL("Digital Mode", 0, 50000, 0)  //dummy channel width values; equals largest channel width (=WFM)
}