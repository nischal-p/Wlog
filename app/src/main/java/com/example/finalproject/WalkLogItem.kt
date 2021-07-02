package com.example.finalproject

import com.google.android.gms.maps.model.LatLng

data class WalkLogItem (
    val description: String = "",
    val date: String = "",
    val trip_log: List<String> = ArrayList<String>(),
    val jog_length: Float = 0.toFloat(),
    val jog_time_seconds: Int = 0,
    var uuid: String = ""
)