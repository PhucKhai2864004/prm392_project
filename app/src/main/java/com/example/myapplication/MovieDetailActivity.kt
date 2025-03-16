package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.adapter.ShowtimeAdapter
import com.example.myapplication.model.Cinema
import com.example.myapplication.model.Movie
import com.example.myapplication.model.Showtime
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MovieDetailActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private lateinit var movie: Movie
    private lateinit var cinemas: List<Cinema>
    private lateinit var showtimes: List<Showtime>
    private lateinit var selectedCinema: Cinema
    private lateinit var selectedDate: Date

    private lateinit var layoutDates: LinearLayout
    private lateinit var spinnerCinema: Spinner
    private lateinit var rvShowtimes: RecyclerView
    private lateinit var rvCast: RecyclerView

    // Danh sách phim giả lập để dự phòng
    private val dummyMovies = listOf(
        Movie("1", "Avengers: Endgame", "Action, Adventure", "180 min", ""),
        Movie("2", "Joker", "Crime, Drama", "122 min", ""),
        Movie("3", "Parasite", "Drama, Thriller", "132 min", ""),
        Movie("4", "1917", "Drama, War", "119 min", ""),
        Movie("5", "Black Widow", "Action, Adventure", "134 min", "", false),
        Movie("6", "No Time to Die", "Action, Thriller", "163 min", "", false),
        Movie("7", "Dune", "Adventure, Sci-Fi", "155 min", "", false),
        Movie("8", "The Batman", "Action, Crime", "176 min", "", false)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie_detail)

        // Get movie ID from intent
        val movieId = intent.getStringExtra("MOVIE_ID") ?: ""
        Log.d("MovieDetailActivity", "Received movie ID: $movieId")

        if (movieId.isEmpty()) {
            Toast.makeText(this, "Movie ID is empty", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize views
        layoutDates = findViewById(R.id.layoutDates)
        spinnerCinema = findViewById(R.id.spinnerCinema)
        rvShowtimes = findViewById(R.id.rvShowtimes)
        rvCast = findViewById(R.id.rvCast)

        // Set up RecyclerViews with empty adapters initially
        setupRecyclerViews()

        // Set up toolbar correctly
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false) // Không hiển thị title mặc định

        // Load movie details from Firestore
        loadMovieDetails(movieId)

        // Load cinemas
        loadCinemas()

        // Set up date selection
        setupDateSelection()

        // Set up cinema selection
        setupCinemaSelection()

        // Set up book button
        findViewById<View>(R.id.btnBookTickets).setOnClickListener {
            if (::selectedCinema.isInitialized && ::showtimes.isInitialized && showtimes.isNotEmpty()) {
                val intent = Intent(this, SeatSelectionActivity::class.java)
                intent.putExtra("MOVIE_ID", movieId)
                intent.putExtra("MOVIE_TITLE", movie.title)
                intent.putExtra("CINEMA_ID", selectedCinema.id)
                intent.putExtra("CINEMA_NAME", selectedCinema.name)
                intent.putExtra("SHOWTIME_ID", showtimes[0].id)
                intent.putExtra("SHOW_DATE", SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(selectedDate))
                intent.putExtra("SHOW_TIME", showtimes[0].time)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please select a showtime", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerViews() {
        // Set up RecyclerView for showtimes with empty adapter initially
        rvShowtimes.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvShowtimes.adapter = ShowtimeAdapter(emptyList()) { /* Empty click handler */ }

        // Set up RecyclerView for cast with empty adapter initially
        rvCast.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        // Tạo một adapter tạm thời cho danh sách diễn viên
        val emptyAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val view = View(parent.context)
                return object : RecyclerView.ViewHolder(view) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                // Do nothing
            }

            override fun getItemCount(): Int = 0
        }
        rvCast.adapter = emptyAdapter
    }

    private fun loadMovieDetails(movieId: String) {
        db.collection("movies").document(movieId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    movie = document.toObject(Movie::class.java)!!

                    // Update UI with movie details
                    findViewById<TextView>(R.id.tvMovieTitle).text = movie.title
                    findViewById<TextView>(R.id.tvMovieGenre).text = movie.genre
                    findViewById<TextView>(R.id.tvMovieDuration).text = movie.duration
                    findViewById<TextView>(R.id.tvSynopsis).text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat."

                    // In a real app, you would use Glide to load the movie poster and backdrop
                    // Glide.with(this).load(movie.posterUrl).into(findViewById(R.id.ivMoviePoster))
                    // Glide.with(this).load(movie.backdropUrl).into(findViewById(R.id.ivMovieBackdrop))

                    // Load cast data
                    loadCastData(movieId)
                } else {
                    Log.e("MovieDetailActivity", "Movie not found in Firestore with ID: $movieId")
                    // Nếu không tìm thấy trong Firestore, thử tìm trong dữ liệu giả lập
                    loadMovieDetailsFromDummyData(movieId)
                }
            }
            .addOnFailureListener { e ->
                Log.e("MovieDetailActivity", "Error loading movie: ${e.message}")
                // Nếu có lỗi khi truy vấn Firestore, thử tìm trong dữ liệu giả lập
                loadMovieDetailsFromDummyData(movieId)
            }
    }

    private fun loadMovieDetailsFromDummyData(movieId: String) {
        // Tìm phim trong danh sách giả lập
        val foundMovie = dummyMovies.find { it.id == movieId }

        if (foundMovie != null) {
            movie = foundMovie

            // Update UI with movie details
            findViewById<TextView>(R.id.tvMovieTitle).text = movie.title
            findViewById<TextView>(R.id.tvMovieGenre).text = movie.genre
            findViewById<TextView>(R.id.tvMovieDuration).text = movie.duration
            findViewById<TextView>(R.id.tvSynopsis).text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat."

            // Load cast data
            loadCastData(movieId)
        } else {
            Log.e("MovieDetailActivity", "Movie not found in dummy data with ID: $movieId")
            Toast.makeText(this, "Movie not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadCastData(movieId: String) {
        // Trong ứng dụng thực tế, bạn sẽ tải dữ liệu diễn viên từ Firestore
        // Ví dụ:
        // db.collection("movies").document(movieId).collection("cast").get()...

        // Tạm thời, chúng ta có thể sử dụng dữ liệu giả
        // Nếu bạn đã có CastAdapter, hãy sử dụng nó ở đây
        // rvCast.adapter = CastAdapter(castList)
    }

    private fun loadCinemas() {
        // In a real app, you would load cinemas from Firestore
        // For simplicity, we'll use dummy data
        cinemas = listOf(
            Cinema("1", "CGV Vincom Center", "Vincom Center, District 1", "Ho Chi Minh City", "123-456-7890"),
            Cinema("2", "CGV Aeon Mall", "Aeon Mall, District 10", "Ho Chi Minh City", "123-456-7891"),
            Cinema("3", "CGV Crescent Mall", "Crescent Mall, District 7", "Ho Chi Minh City", "123-456-7892")
        )

        // Set up cinema spinner
        val cinemaNames = cinemas.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cinemaNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCinema.adapter = adapter
    }

    private fun setupDateSelection() {
        // Show next 7 days
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

        for (i in 0 until 7) {
            val date = calendar.time

            // Create date view
            val dateView = layoutInflater.inflate(R.layout.item_date, layoutDates, false)
            val tvDay = dateView.findViewById<TextView>(R.id.tvDay)
            val tvDate = dateView.findViewById<TextView>(R.id.tvDate)

            tvDay.text = dayFormat.format(date)
            tvDate.text = dateFormat.format(date)

            // Set selected state for first date
            if (i == 0) {
                dateView.isSelected = true
                selectedDate = date
            }

            // Set click listener
            dateView.setOnClickListener {
                // Deselect all dates
                for (j in 0 until layoutDates.childCount) {
                    layoutDates.getChildAt(j).isSelected = false
                }

                // Select this date
                dateView.isSelected = true
                selectedDate = date

                // Load showtimes for selected date and cinema
                if (::selectedCinema.isInitialized) {
                    loadShowtimes(selectedCinema.id, date)
                }
            }

            layoutDates.addView(dateView)

            // Move to next day
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    private fun setupCinemaSelection() {
        spinnerCinema.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCinema = cinemas[position]
                loadShowtimes(selectedCinema.id, selectedDate)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun loadShowtimes(cinemaId: String, date: Date) {
        // In a real app, you would load showtimes from Firestore
        // For simplicity, we'll use dummy data
        showtimes = listOf(
            Showtime("1", "1", cinemaId, date, "10:00 AM", List(50) { "A${it+1}" }, listOf(), 10.0),
            Showtime("2", "1", cinemaId, date, "1:30 PM", List(50) { "A${it+1}" }, listOf(), 10.0),
            Showtime("3", "1", cinemaId, date, "4:00 PM", List(50) { "A${it+1}" }, listOf(), 10.0),
            Showtime("4", "1", cinemaId, date, "7:30 PM", List(50) { "A${it+1}" }, listOf(), 12.0),
            Showtime("5", "1", cinemaId, date, "10:00 PM", List(50) { "A${it+1}" }, listOf(), 12.0)
        )

        // Update UI with showtimes
        val adapter = ShowtimeAdapter(showtimes) { showtime ->
            val intent = Intent(this, SeatSelectionActivity::class.java)
            intent.putExtra("MOVIE_ID", movie.id)
            intent.putExtra("MOVIE_TITLE", movie.title)
            intent.putExtra("CINEMA_ID", selectedCinema.id)
            intent.putExtra("CINEMA_NAME", selectedCinema.name)
            intent.putExtra("SHOWTIME_ID", showtime.id)
            intent.putExtra("SHOW_DATE", SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(selectedDate))
            intent.putExtra("SHOW_TIME", showtime.time)
            startActivity(intent)
        }
        rvShowtimes.adapter = adapter
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}