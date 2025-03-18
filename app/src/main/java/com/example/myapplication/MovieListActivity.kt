package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.adapter.MovieAdapter
import com.example.myapplication.model.Movie
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MovieListActivity : AppCompatActivity() {

    private val db = Firebase.firestore

    private lateinit var tvListTitle: TextView
    private lateinit var rvMovies: RecyclerView
    private lateinit var tvNoMovies: TextView
    private lateinit var etSearch: EditText
    private lateinit var bottomNavigation: BottomNavigationView

    private var isNowShowing = true
    private var allMovies = listOf<Movie>()
    private var filteredMovies = listOf<Movie>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie_list)

        // Thiết lập toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Khởi tạo views
        tvListTitle = findViewById(R.id.tvListTitle)
        rvMovies = findViewById(R.id.rvMovies)
        tvNoMovies = findViewById(R.id.tvNoMovies)
        etSearch = findViewById(R.id.etSearch)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        // Lấy loại danh sách phim từ intent
        isNowShowing = intent.getBooleanExtra("IS_NOW_SHOWING", true)

        // Cập nhật tiêu đề
        tvListTitle.text = if (isNowShowing) "Phim đang chiếu" else "Phim sắp chiếu"

        // Thiết lập sự kiện cho thanh tìm kiếm
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                filterMovies(s.toString())
            }
        })

        // Thiết lập bottom navigation
        setupBottomNavigation()

        // Tải danh sách phim
        loadMovies()
    }

    private fun setupBottomNavigation() {
        // Đánh dấu mục đang chọn
        bottomNavigation.selectedItemId = R.id.nav_home

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_cinemas -> {
                    startActivity(Intent(this, CinemaListActivity::class.java))
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

    private fun loadMovies() {
        db.collection("movies")
            .get()
            .addOnSuccessListener { documents ->
                allMovies = documents.toObjects(Movie::class.java)

                // Lọc phim theo loại
                filteredMovies = allMovies.filter { it.nowShowing == isNowShowing }

                updateMovieList()
            }
            .addOnFailureListener { e ->
                tvNoMovies.visibility = View.VISIBLE
                rvMovies.visibility = View.GONE
            }
    }

    private fun filterMovies(query: String) {
        if (query.isEmpty()) {
            // Nếu không có từ khóa tìm kiếm, hiển thị tất cả phim theo loại
            filteredMovies = allMovies.filter { it.nowShowing == isNowShowing }
        } else {
            // Lọc phim theo từ khóa tìm kiếm và loại
            filteredMovies = allMovies.filter {
                it.nowShowing == isNowShowing &&
                        it.title.contains(query, ignoreCase = true)
            }
        }

        updateMovieList()
    }

    private fun updateMovieList() {
        if (filteredMovies.isEmpty()) {
            tvNoMovies.visibility = View.VISIBLE
            rvMovies.visibility = View.GONE
        } else {
            tvNoMovies.visibility = View.GONE
            rvMovies.visibility = View.VISIBLE

            // Hiển thị danh sách phim
            val adapter = MovieAdapter(
                filteredMovies,
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}