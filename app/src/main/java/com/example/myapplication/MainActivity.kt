package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
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
import com.example.myapplication.model.Showtime

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
        setupBottomNav()

        // Set up profile icon click
        findViewById<View>(R.id.ivProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Set up view all clicks
        findViewById<View>(R.id.tvViewAllNowShowing).setOnClickListener {
            Toast.makeText(this, "Xem tất cả phim đang chiếu", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.tvViewAllComingSoon).setOnClickListener {
            Toast.makeText(this, "Xem tất cả phim sắp chiếu", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Kiểm tra nếu người dùng là admin
        if (auth.currentUser?.email == "admin@example.com") {
            menuInflater.inflate(R.menu.admin_menu, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Xử lý khi người dùng chọn menu item
        if (item.itemId == R.id.menu_admin) {
            // Mở AdminActivity
            startActivity(Intent(this, AdminActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
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
            Movie("1", "Avengers: Endgame", "Hành động, Phiêu lưu", "180 phút", ""),
            Movie("2", "Joker", "Tội phạm, Kịch", "122 phút", ""),
            Movie("3", "Parasite", "Kịch, Kinh dị", "132 phút", ""),
            Movie("4", "1917", "Kịch, Chiến tranh", "119 phút", ""),
            Movie("5", "Black Widow", "Hành động, Phiêu lưu", "134 phút", "", false),
            Movie("6", "No Time to Die", "Hành động, Kinh dị", "163 phút", "", false),
            Movie("7", "Dune", "Phiêu lưu, Khoa học viễn tưởng", "155 phút", "", false),
            Movie("8", "The Batman", "Hành động, Tội phạm", "176 phút", "", false)
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
                val nowShowingMovies = ArrayList<Movie>()
                for (document in documents) {
                    val movie = document.toObject(Movie::class.java)
                    nowShowingMovies.add(movie)
                }

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
                setupDummyMovies()
            }

        // Tải phim sắp chiếu
        db.collection("movies")
            .whereEqualTo("nowShowing", false)
            .get()
            .addOnSuccessListener { documents ->
                val comingSoonMovies = ArrayList<Movie>()
                for (document in documents) {
                    val movie = document.toObject(Movie::class.java)
                    comingSoonMovies.add(movie)
                }

                val comingSoonAdapter = MovieAdapter(
                    comingSoonMovies,
                    onMovieClick = { movie ->
                        Toast.makeText(this, "Phim đã chọn: ${movie.title}", Toast.LENGTH_SHORT).show()
                    },
                    onBookClick = { movie ->
                        Toast.makeText(this, "Sắp chiếu: ${movie.title}", Toast.LENGTH_SHORT).show()
                    }
                )

                rvComingSoon.adapter = comingSoonAdapter
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Error loading coming soon movies: ${e.message}")
            }
    }

    private fun setupDummyMovies() {
        // Dữ liệu phim mẫu (sử dụng khi không tải được từ Firestore)
        val nowShowingMovies = listOf(
            Movie("1", "Avengers: Endgame", "Hành động, Phiêu lưu", "180 phút", ""),
            Movie("2", "Joker", "Tội phạm, Kịch", "122 phút", ""),
            Movie("3", "Parasite", "Kịch, Kinh dị", "132 phút", ""),
            Movie("4", "1917", "Kịch, Chiến tranh", "119 phút", "")
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
            Movie("5", "Black Widow", "Hành động, Phiêu lưu", "134 phút", "", false),
            Movie("6", "No Time to Die", "Hành động, Kinh dị", "163 phút", "", false),
            Movie("7", "Dune", "Phiêu lưu, Khoa học viễn tưởng", "155 phút", "", false),
            Movie("8", "The Batman", "Hành động, Tội phạm", "176 phút", "", false)
        )

        val comingSoonAdapter = MovieAdapter(
            comingSoonMovies,
            onMovieClick = { movie ->
                Toast.makeText(this, "Phim đã chọn: ${movie.title}", Toast.LENGTH_SHORT).show()
            },
            onBookClick = { movie ->
                Toast.makeText(this, "Sắp chiếu: ${movie.title}", Toast.LENGTH_SHORT).show()
            }
        )

        rvComingSoon.adapter = comingSoonAdapter
    }

    private fun setupBottomNav() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Already on home
                    true
                }
                R.id.nav_movies -> {
                    Toast.makeText(this, "Phim", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_cinemas -> {
                    Toast.makeText(this, "Rạp chiếu", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_tickets -> {
                    Toast.makeText(this, "Vé của tôi", Toast.LENGTH_SHORT).show()
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
    private fun setupShowtimes() {
        // Tạo các suất chiếu mẫu nếu chưa có trong Firestore
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // Tạo danh sách tất cả các ghế
        val allSeats = mutableListOf<String>()
        val rows = 8
        val cols = 10

        for (i in 0 until rows) {
            val rowChar = ('A' + i).toString()
            for (j in 1..cols) {
                allSeats.add("$rowChar$j")
            }
        }

        // Tạo một số ghế đã đặt ngẫu nhiên
        val randomBookedSeats = listOf("A1", "A2", "B5", "C7", "D3", "D4", "E8", "F2", "G5", "H10")

        // Tạo danh sách ghế còn trống
        val availableSeats = allSeats.filter { !randomBookedSeats.contains(it) }

        // Tạo các suất chiếu cho 7 ngày tới
        for (i in 0 until 7) {
            val date = calendar.time
            val dateStr = dateFormat.format(date)

            // Tạo các suất chiếu cho ngày này
            val showtimes = listOf(
                Showtime(
                    id = "showtime_${movieId}_${dateStr}_1000",
                    movieId = movieId,
                    cinemaId = "1",
                    date = date,
                    time = "10:00",
                    availableSeats = availableSeats,
                    bookedSeats = randomBookedSeats,
                    price = 100000.0
                ),
                Showtime(
                    id = "showtime_${movieId}_${dateStr}_1330",
                    movieId = movieId,
                    cinemaId = "1",
                    date = date,
                    time = "13:30",
                    availableSeats = availableSeats,
                    bookedSeats = randomBookedSeats,
                    price = 100000.0
                ),
                Showtime(
                    id = "showtime_${movieId}_${dateStr}_1600",
                    movieId = movieId,
                    cinemaId = "1",
                    date = date,
                    time = "16:00",
                    availableSeats = availableSeats,
                    bookedSeats = randomBookedSeats,
                    price = 100000.0
                ),
                Showtime(
                    id = "showtime_${movieId}_${dateStr}_1930",
                    movieId = movieId,
                    cinemaId = "1",
                    date = date,
                    time = "19:30",
                    availableSeats = availableSeats,
                    bookedSeats = randomBookedSeats,
                    price = 120000.0
                ),
                Showtime(
                    id = "showtime_${movieId}_${dateStr}_2200",
                    movieId = movieId,
                    cinemaId = "1",
                    date = date,
                    time = "22:00",
                    availableSeats = availableSeats,
                    bookedSeats = randomBookedSeats,
                    price = 120000.0
                )
            )

            // Lưu các suất chiếu vào Firestore
            for (showtime in showtimes) {
                db.collection("showtimes").document(showtime.id)
                    .set(showtime)
                    .addOnSuccessListener {
                        Log.d("MovieDetailActivity", "Showtime added: ${showtime.id}")
                    }
                    .addOnFailureListener { e ->
                        Log.e("MovieDetailActivity", "Error adding showtime: ${e.message}")
                    }
            }

            // Chuyển đến ngày tiếp theo
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
    }
}