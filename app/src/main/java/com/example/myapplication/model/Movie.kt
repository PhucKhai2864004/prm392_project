package com.example.myapplication.model

import java.util.Date

data class Movie(
    val id: String = "",
    val title: String = "",
    val genre: String = "",
    val duration: String = "",
    val posterUrl: String = "",
    val nowShowing: Boolean = true,
    // Thêm các trường mới
    val releaseDate: Date? = null,
    val rating: String = "", // Xếp hạng như PG, PG-13, R, v.v.
    val imdbRating: String = "", // Điểm IMDb (ví dụ: 8.5/10)
    val synopsis: String = "", // Nội dung phim
    val director: String = "", // Đạo diễn
    val cast: List<String> = listOf() // Danh sách diễn viên
)


