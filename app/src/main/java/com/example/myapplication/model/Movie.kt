package com.example.myapplication.model

data class Movie(
    val id: String = "",
    val title: String = "",
    val genre: String = "",
    val duration: String = "",
    val posterUrl: String = "",
    val nowShowing: Boolean = true
)

