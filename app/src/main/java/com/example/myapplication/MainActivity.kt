package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.myapplication.adapter.BannerAdapter
import com.example.myapplication.adapter.MovieAdapter
import com.example.myapplication.model.Banner
import com.example.myapplication.model.Movie
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    private lateinit var viewPagerBanner: ViewPager2
    private lateinit var tabIndicator: TabLayout
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
        viewPagerBanner = findViewById(R.id.viewPagerBanner)
        tabIndicator = findViewById(R.id.tabIndicator)
        rvNowShowing = findViewById(R.id.rvNowShowing)
        rvComingSoon = findViewById(R.id.rvComingSoon)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        ivProfile = findViewById(R.id.ivProfile)

        // Set up banner slider
        setupBannerSlider()

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
            // TODO: Navigate to all now showing movies
        }

        findViewById<TextView>(R.id.tvViewAllComingSoon).setOnClickListener {
            // TODO: Navigate to all coming soon movies
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

    private fun setupBannerSlider() {
        // Dữ liệu banner mẫu
        val banners = listOf(
            Banner("1", "https://www.cgv.vn/media/banner/cache/1/b58515f018eb873dafa430b6f9ae0c1e/9/8/980x448_1.jpg"),
            Banner("2", "https://www.cgv.vn/media/banner/cache/1/b58515f018eb873dafa430b6f9ae0c1e/9/8/980x448_141.jpg"),
            Banner("3", "https://www.cgv.vn/media/banner/cache/1/b58515f018eb873dafa430b6f9ae0c1e/9/8/980x448_140.jpg")
        )

        val adapter = BannerAdapter(banners) { banner ->
            // TODO: Handle banner click
        }

        viewPagerBanner.adapter = adapter

        // Set up tab indicator
        TabLayoutMediator(tabIndicator, viewPagerBanner) { _, _ ->
            // No text for tab
        }.attach()

        // Auto slide
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val currentItem = viewPagerBanner.currentItem
                val nextItem = if (currentItem == banners.size - 1) 0 else currentItem + 1
                viewPagerBanner.setCurrentItem(nextItem, true)
                handler.postDelayed(this, 3000)
            }
        }
        handler.postDelayed(runnable, 3000)
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
                // Nếu có lỗi, sử dụng dữ liệu mẫu
                val dummyMovies = listOf(
                    Movie("1", "Avengers: Endgame", "Hành động, Phiêu lưu", "180 phút", "", true),
                    Movie("2", "Joker", "Tội phạm, Kịch", "122 phút", "", true),
                    Movie("3", "Parasite", "Kịch, Kinh dị", "132 phút", "", true),
                    Movie("4", "1917", "Kịch, Chiến tranh", "119 phút", "", false),
                    Movie("5", "The Batman", "Hành động, Tội phạm", "176 phút", "", false),
                    Movie("6", "Dune", "Khoa học viễn tưởng", "155 phút", "", false)
                )

                // Lọc phim đang chiếu và sắp chiếu
                val nowShowingMovies = dummyMovies.filter { it.nowShowing }
                val comingSoonMovies = dummyMovies.filter { !it.nowShowing }

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
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Already on home
                    true
                }
                R.id.nav_movies -> {
                    // TODO: Navigate to movies screen
                    true
                }
                R.id.nav_cinemas -> {
                    // TODO: Navigate to cinemas screen
                    true
                }
                R.id.nav_tickets -> {
                    // TODO: Navigate to tickets screen
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