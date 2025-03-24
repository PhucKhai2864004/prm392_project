package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.adapter.MovieAdapter
import com.example.myapplication.model.Movie
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    private lateinit var rvNowShowing: RecyclerView
    private lateinit var rvComingSoon: RecyclerView
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var ivProfile: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Check if user is signed in
        if (auth.currentUser == null) {
            // Not signed in, launch the Login activity
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Initialize views
        rvNowShowing = findViewById(R.id.rvNowShowing)
        rvComingSoon = findViewById(R.id.rvComingSoon)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        ivProfile = findViewById(R.id.ivProfile)

        // Set up movie lists
        loadMovies()

        // Set up bottom navigation
        setupBottomNavigation()

        // Set up profile icon click
        ivProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Set up view all buttons
        findViewById<TextView>(R.id.tvViewAllNowShowing).setOnClickListener {
            val intent = Intent(this, MovieListActivity::class.java)
            intent.putExtra("IS_NOW_SHOWING", true)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.tvViewAllComingSoon).setOnClickListener {
            val intent = Intent(this, MovieListActivity::class.java)
            intent.putExtra("IS_NOW_SHOWING", false)
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.admin_menu, menu)

        // Chỉ hiển thị menu admin cho tài khoản admin
        menu.findItem(R.id.menu_admin).isVisible = auth.currentUser?.email == "admin@example.com"

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_admin -> {
                startActivity(Intent(this, AdminActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadMovies() {
        // Tải danh sách phim từ Firestore
        db.collection("movies")
            .get()
            .addOnSuccessListener { documents ->
                val allMovies = documents.toObjects(Movie::class.java)

                // Lọc phim đang chiếu và sắp chiếu
                val nowShowingMovies = allMovies.filter { it.nowShowing }
                val comingSoonMovies = allMovies.filter { !it.nowShowing }

                // Hiển thị danh sách phim đang chiếu
                val nowShowingAdapter = MovieAdapter(
                    nowShowingMovies,
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
                rvNowShowing.adapter = nowShowingAdapter

                // Hiển thị danh sách phim sắp chiếu
                val comingSoonAdapter = MovieAdapter(
                    comingSoonMovies,
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
                rvComingSoon.adapter = comingSoonAdapter
            }
            .addOnFailureListener { e ->
                // Xử lý lỗi khi tải danh sách phim
                android.widget.Toast.makeText(this, "Lỗi khi tải danh sách phim: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Already on home
                    true
                }
                R.id.nav_cinemas -> {
                    // Chuyển đến trang danh sách rạp phim
                    startActivity(Intent(this, CinemaListActivity::class.java))
                    true
                }
                R.id.nav_tickets -> {
                    // Chuyển đến trang vé của tôi
                    startActivity(Intent(this, MyTicketsActivity::class.java))
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
}

