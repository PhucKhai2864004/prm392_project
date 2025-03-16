package com.example.myapplication.model

import java.util.Date

data class Booking(
    val id: String = "",
    val userId: String = "",
    val movieId: String = "",
    val movieTitle: String = "",
    val cinemaId: String = "",
    val cinemaName: String = "",
    val showDate: Date = Date(),
    val showTime: String = "",
    val seats: List<String> = listOf(),
    val totalAmount: Double = 0.0,
    val paymentMethod: String = "",
    val bookingDate: Date = Date(),
    val status: String = "Confirmed"
)