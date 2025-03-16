package com.example.myapplication.model

import java.util.Date

data class Showtime(
    val id: String = "",
    val movieId: String = "",
    val cinemaId: String = "",
    val date: Date = Date(),
    val time: String = "",
    val availableSeats: List<String> = listOf(),
    val bookedSeats: List<String> = listOf(),
    val price: Double = 0.0
)