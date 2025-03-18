package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.adapter.MovieAdapter
import com.example.myapplication.model.Cinema
import com.example.myapplication.model.Movie
import com.example.myapplication.model.Showtime
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Date

class CinemaDetailActivity : AppCompatActivity() {

    private val db = Firebase.firestore

    private lateinit var tvCinemaName: TextView
    private lateinit var tvCinemaAddress: TextView
    private lateinit var tvCinemaCity: TextView
    private lateinit var tvCinemaPhone: TextView
    private lateinit var rvMovies: RecyclerView
    private lateinit var tvNoMovies: TextView
    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var cinemaId: String
    private lateinit var cinema: Cinema
    private var movies = listOf<Movie>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cinema_detail)

        // Thiết lập toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Lấy ID rạp phim từ intent
        cinemaId = intent.getStringExtra("CINEMA_ID") ?: ""
        if (cinemaId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin rạp phim", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Khởi tạo views
        tvCinemaName = findViewById(R.id.tvCinemaName)
        tvCinemaAddress = findViewById(R.id.tvCinemaAddress)
        tvCinemaCity = findViewById(R.id.tvCinemaCity)
        tvCinemaPhone = findViewById(R.id.tvCinemaPhone)
        rvMovies = findViewById(R.id.rvMovies)
        tvNoMovies = findViewById(R.id.tvNoMovies)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        // Thiết lập bottom navigation
        setupBottomNavigation()

        // Tải thông tin rạp phim
        loadCinemaDetails()
    }

    private fun setupBottomNavigation() {
        // Đánh dấu mục đang chọn
        bottomNavigation.selectedItemId = R.id.nav_cinemas

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_cinemas -> {
                    onBackPressed()
                    true
                }
                R.id.nav_tickets -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun loadCinemaDetails() {
        db.collection("cinemas").document(cinemaId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    cinema = document.toObject(Cinema::class.java)!!

                    // Cập nhật UI với thông tin rạp phim
                    tvCinemaName.text = cinema.name
                    tvCinemaAddress.text = cinema.address
                    tvCinemaCity.text = cinema.city
                    tvCinemaPhone.text = cinema.phoneNumber

                    // Cập nhật tiêu đề toolbar
                    supportActionBar?.title = cinema.name

                    // Tải danh sách phim đang chiếu tại rạp
                    loadMoviesForCinema()
                } else {
                    Toast.makeText(this, "Không tìm thấy thông tin rạp phim", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi khi tải thông tin rạp phim: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun loadMoviesForCinema() {
        // Lấy ngày hiện tại
        val currentDate = Date()

        // Tìm tất cả lịch chiếu của rạp này
        db.collection("showtimes")
            .whereEqualTo("cinemaId", cinemaId)
            .get()
            .addOnSuccessListener { documents ->
                val showtimes = documents.toObjects(Showtime::class.java)

                // Lấy danh sách ID phim từ các lịch chiếu
                val movieIds = showtimes.map { it.movieId }.distinct()

                if (movieIds.isEmpty()) {
                    tvNoMovies.visibility = View.VISIBLE
                    rvMovies.visibility = View.GONE
                } else {
                    // Tải thông tin chi tiết của các phim
                    db.collection("movies")
                        .whereIn("id", movieIds)
                        .get()
                        .addOnSuccessListener { movieDocuments ->
                            movies = movieDocuments.toObjects(Movie::class.java)

                            if (movies.isEmpty()) {
                                tvNoMovies.visibility = View.VISIBLE
                                rvMovies.visibility = View.GONE
                            } else {
                                tvNoMovies.visibility = View.GONE
                                rvMovies.visibility = View.VISIBLE

                                // Hiển thị danh sách phim
                                val adapter = MovieAdapter(
                                    movies,
                                    onMovieClick = { movie ->
                                        val intent = Intent(this, MovieDetailActivity::class.java)
                                        intent.putExtra("MOVIE_ID", movie.id)
                                        startActivity(intent)
                                    },
                                    onBookClick = { movie ->
                                        val intent = Intent(this, MovieDetailActivity::class.java)
                                        intent.putExtra("MOVIE_ID", movie.id)
                                        startActivity(intent)
                                    }
                                )
                                rvMovies.adapter = adapter
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("CinemaDetailActivity", "Error loading movies: ${e.message}")
                            tvNoMovies.visibility = View.VISIBLE
                            rvMovies.visibility = View.GONE
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("CinemaDetailActivity", "Error loading showtimes: ${e.message}")
                tvNoMovies.visibility = View.VISIBLE
                rvMovies.visibility = View.GONE
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

