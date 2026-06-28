package com.pz64.rfa.data


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
