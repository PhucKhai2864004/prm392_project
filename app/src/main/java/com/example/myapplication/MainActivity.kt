package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Thiết lập toolbar nếu có
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        if (toolbar != null) {
            setSupportActionBar(toolbar)
        }

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

        // Thêm dữ liệu phim vào Firestore nếu chưa có
        checkAndAddMoviesToFirestore()

        // Set up banner slider
        setupBannerSlider()

        // Set up movie lists
        setupMovieLists()

        // Set up bottom navigation
        setupBottomNavigation()

        // Set up profile icon click
        findViewById<View>(R.id.ivProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Set up view all clicks
        findViewById<View>(R.id.tvViewAllNowShowing).setOnClickListener {
            Toast.makeText(this, "View All Now Showing clicked", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.tvViewAllComingSoon).setOnClickListener {
            Toast.makeText(this, "View All Coming Soon clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndAddMoviesToFirestore() {
        // Kiểm tra xem đã có dữ liệu phim trong Firestore chưa
        db.collection("movies").get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // Nếu chưa có dữ liệu, thêm dữ liệu phim mẫu
                    addMoviesToFirestore()
                } else {
                    Log.d("MainActivity", "Movies already exist in Firestore: ${documents.size()}")
                }
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Error checking movies: ${e.message}")
            }
    }

    private fun addMoviesToFirestore() {
        // Danh sách phim mẫu
        val movies = listOf(
            Movie("1", "Avengers: Endgame", "Action, Adventure", "180 min", ""),
            Movie("2", "Joker", "Crime, Drama", "122 min", ""),
            Movie("3", "Parasite", "Drama, Thriller", "132 min", ""),
            Movie("4", "1917", "Drama, War", "119 min", ""),
            Movie("5", "Black Widow", "Action, Adventure", "134 min", "", false),
            Movie("6", "No Time to Die", "Action, Thriller", "163 min", "", false),
            Movie("7", "Dune", "Adventure, Sci-Fi", "155 min", "", false),
            Movie("8", "The Batman", "Action, Crime", "176 min", "", false)
        )

        // Thêm từng phim vào Firestore
        for (movie in movies) {
            db.collection("movies").document(movie.id)
                .set(movie)
                .addOnSuccessListener {
                    Log.d("MainActivity", "Movie added: ${movie.title}")
                }
                .addOnFailureListener { e ->
                    Log.e("MainActivity", "Error adding movie ${movie.title}: ${e.message}")
                }
        }
    }

    private fun setupBannerSlider() {
        val banners = listOf(
            Banner("1", "https://example.com/banner1.jpg"),
            Banner("2", "https://example.com/banner2.jpg"),
            Banner("3", "https://example.com/banner3.jpg")
        )

        val bannerAdapter = BannerAdapter(banners) { banner ->
            Toast.makeText(this, "Banner clicked: ${banner.id}", Toast.LENGTH_SHORT).show()
        }

        viewPagerBanner.adapter = bannerAdapter

        // Connect TabLayout with ViewPager2
        TabLayoutMediator(tabIndicator, viewPagerBanner) { _, _ -> }.attach()

        // Auto-scroll banner
        val handler = android.os.Handler(mainLooper)
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

    private fun setupMovieLists() {
        // Tải danh sách phim từ Firestore
        loadMoviesFromFirestore()
    }

    private fun loadMoviesFromFirestore() {
        // Tải phim đang chiếu
        db.collection("movies")
            .whereEqualTo("nowShowing", true)
            .get()
            .addOnSuccessListener { documents ->
                val nowShowingMovies = documents.toObjects(Movie::class.java)

                val nowShowingAdapter = MovieAdapter(
                    nowShowingMovies,
                    onMovieClick = { movie ->
                        Log.d("MainActivity", "Movie clicked: ${movie.id} - ${movie.title}")
                        val intent = Intent(this, MovieDetailActivity::class.java)
                        intent.putExtra("MOVIE_ID", movie.id)
                        startActivity(intent)
                    },
                    onBookClick = { movie ->
                        Log.d("MainActivity", "Book clicked: ${movie.id} - ${movie.title}")
                        val intent = Intent(this, MovieDetailActivity::class.java)
                        intent.putExtra("MOVIE_ID", movie.id)
                        startActivity(intent)
                    }
                )

                rvNowShowing.adapter = nowShowingAdapter
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Error loading now showing movies: ${e.message}")
                // Sử dụng dữ liệu mẫu nếu không tải được từ Firestore
                setupMovieListsWithDummyData()
            }

        // Tải phim sắp chiếu
        db.collection("movies")
            .whereEqualTo("nowShowing", false)
            .get()
            .addOnSuccessListener { documents ->
                val comingSoonMovies = documents.toObjects(Movie::class.java)

                val comingSoonAdapter = MovieAdapter(
                    comingSoonMovies,
                    onMovieClick = { movie ->
                        Toast.makeText(this, "Movie clicked: ${movie.title}", Toast.LENGTH_SHORT).show()
                    },
                    onBookClick = { movie ->
                        Toast.makeText(this, "Coming soon: ${movie.title}", Toast.LENGTH_SHORT).show()
                    }
                )

                rvComingSoon.adapter = comingSoonAdapter
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Error loading coming soon movies: ${e.message}")
            }
    }

    private fun setupMovieListsWithDummyData() {
        // Dữ liệu phim mẫu (sử dụng khi không tải được từ Firestore)
        val nowShowingMovies = listOf(
            Movie("1", "Avengers: Endgame", "Action, Adventure", "180 min", ""),
            Movie("2", "Joker", "Crime, Drama", "122 min", ""),
            Movie("3", "Parasite", "Drama, Thriller", "132 min", ""),
            Movie("4", "1917", "Drama, War", "119 min", "")
        )

        val nowShowingAdapter = MovieAdapter(
            nowShowingMovies,
            onMovieClick = { movie ->
                Log.d("MainActivity", "Movie clicked: ${movie.id} - ${movie.title}")
                val intent = Intent(this, MovieDetailActivity::class.java)
                intent.putExtra("MOVIE_ID", movie.id)
                startActivity(intent)
            },
            onBookClick = { movie ->
                Log.d("MainActivity", "Book clicked: ${movie.id} - ${movie.title}")
                val intent = Intent(this, MovieDetailActivity::class.java)
                intent.putExtra("MOVIE_ID", movie.id)
                startActivity(intent)
            }
        )

        rvNowShowing.adapter = nowShowingAdapter

        // Coming Soon Movies
        val comingSoonMovies = listOf(
            Movie("5", "Black Widow", "Action, Adventure", "134 min", "", false),
            Movie("6", "No Time to Die", "Action, Thriller", "163 min", "", false),
            Movie("7", "Dune", "Adventure, Sci-Fi", "155 min", "", false),
            Movie("8", "The Batman", "Action, Crime", "176 min", "", false)
        )

        val comingSoonAdapter = MovieAdapter(
            comingSoonMovies,
            onMovieClick = { movie ->
                Toast.makeText(this, "Movie clicked: ${movie.title}", Toast.LENGTH_SHORT).show()
            },
            onBookClick = { movie ->
                Toast.makeText(this, "Coming soon: ${movie.title}", Toast.LENGTH_SHORT).show()
            }
        )

        rvComingSoon.adapter = comingSoonAdapter
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Already on home
                    true
                }
                R.id.nav_movies -> {
                    Toast.makeText(this, "Movies clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_cinemas -> {
                    Toast.makeText(this, "Cinemas clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_tickets -> {
                    Toast.makeText(this, "My Tickets clicked", Toast.LENGTH_SHORT).show()
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

