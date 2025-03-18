package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.model.Cinema
import com.example.myapplication.model.Movie
import com.example.myapplication.model.Showtime
import com.example.myapplication.utils.ImageCache
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class MovieDetailActivity : AppCompatActivity() {

    private val db = Firebase.firestore

    private lateinit var movie: Movie
    private lateinit var rvCast: RecyclerView
    private lateinit var rvShowtimes: RecyclerView
    private lateinit var spinnerCinema: Spinner

    private lateinit var selectedCinema: Cinema
    private lateinit var selectedDate: Date
    private var showtimes: List<Showtime> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie_detail)

        // Thiết lập toolbar đúng cách
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Lấy ID phim từ intent
        val movieId = intent.getStringExtra("MOVIE_ID") ?: ""
        if (movieId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy phim", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Khởi tạo views
        rvCast = findViewById(R.id.rvCast)
        rvShowtimes = findViewById(R.id.rvShowtimes)
        spinnerCinema = findViewById(R.id.spinnerCinema)

        // Tải thông tin phim
        loadMovieDetails(movieId)

        // Thiết lập nút đặt vé
        findViewById<View>(R.id.btnBookTickets).setOnClickListener {
            if (!::selectedCinema.isInitialized || showtimes.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn rạp và suất chiếu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Vui lòng chọn suất chiếu", Toast.LENGTH_SHORT).show()
        }
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

                    // Cập nhật thông tin chi tiết phim
                    if (movie.releaseDate != null) {
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        findViewById<TextView>(R.id.tvReleaseDate).text = dateFormat.format(movie.releaseDate)
                    } else {
                        findViewById<TextView>(R.id.tvReleaseDate).text = "Chưa có thông tin"
                    }

                    findViewById<TextView>(R.id.tvRating).text = if (movie.rating.isNotEmpty()) movie.rating else "Chưa xếp hạng"
                    findViewById<TextView>(R.id.tvImdbRating).text = if (movie.imdbRating.isNotEmpty()) movie.imdbRating else "Chưa có đánh giá"
                    findViewById<TextView>(R.id.tvSynopsis).text = if (movie.synopsis.isNotEmpty()) movie.synopsis else "Chưa có nội dung phim"

                    // Tải hình ảnh poster và backdrop
                    val ivMovieBackdrop = findViewById<ImageView>(R.id.ivMovieBackdrop)
                    if (movie.posterUrl.isNotEmpty()) {
                        ImageCache.loadImageWithoutCache(
                            this,
                            movie.posterUrl,
                            ivMovieBackdrop,
                            R.drawable.ic_launcher_background,
                            R.drawable.ic_launcher_foreground
                        )
                    }

                    // Load cast data
                    loadCastData()

                    // Load cinema data
                    loadCinemaData()

                    // Setup dates
                    setupDates()
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
        // Dữ liệu phim mẫu
        val dummyMovies = listOf(
            Movie("1", "Avengers: Endgame", "Hành động, Phiêu lưu", "180 phút", ""),
            Movie("2", "Joker", "Tội phạm, Kịch", "122 phút", ""),
            Movie("3", "Parasite", "Kịch, Kinh dị", "132 phút", ""),
            Movie("4", "1917", "Kịch, Chiến tranh", "119 phút", "")
        )

        // Tìm phim theo ID
        val foundMovie = dummyMovies.find { it.id == movieId }
        if (foundMovie != null) {
            movie = foundMovie

            // Update UI with movie details
            findViewById<TextView>(R.id.tvMovieTitle).text = movie.title
            findViewById<TextView>(R.id.tvMovieGenre).text = movie.genre
            findViewById<TextView>(R.id.tvMovieDuration).text = movie.duration

            // Load cast data
            loadCastData()

            // Load cinema data
            loadCinemaData()

            // Setup dates
            setupDates()
        } else {
            Toast.makeText(this, "Không tìm thấy phim", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadCastData() {
        if (movie.cast.isNotEmpty()) {
            // Hiển thị danh sách diễn viên từ movie.cast
            // Ví dụ: tạo một adapter đơn giản để hiển thị danh sách diễn viên
            val castAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                inner class CastViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
                    val tvCastName: TextView = itemView.findViewById(android.R.id.text1)
                }

                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(android.R.layout.simple_list_item_1, parent, false)
                    return CastViewHolder(view)
                }

                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    val castName = movie.cast[position]
                    (holder as CastViewHolder).tvCastName.text = castName
                }

                override fun getItemCount(): Int = movie.cast.size
            }

            rvCast.adapter = castAdapter
        } else {
            // Nếu không có dữ liệu diễn viên, hiển thị thông báo
            val emptyAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                inner class EmptyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
                    val tvEmpty: TextView = itemView.findViewById(android.R.id.text1)
                }

                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(android.R.layout.simple_list_item_1, parent, false)
                    return EmptyViewHolder(view)
                }

                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    (holder as EmptyViewHolder).tvEmpty.text = "Chưa có thông tin diễn viên"
                }

                override fun getItemCount(): Int = 1
            }

            rvCast.adapter = emptyAdapter
        }
    }

    private fun loadCinemaData() {
        // Tải danh sách rạp từ Firestore
        db.collection("cinemas")
            .get()
            .addOnSuccessListener { documents ->
                val cinemas = ArrayList<Cinema>()
                for (document in documents) {
                    val cinema = document.toObject(Cinema::class.java)
                    cinemas.add(cinema)
                }

                if (cinemas.isEmpty()) {
                    // Nếu không có dữ liệu rạp, thêm dữ liệu mẫu
                    val dummyCinemas = listOf(
                        Cinema("1", "CGV Vincom Center", "Số 72 Lê Thánh Tôn, Bến Nghé, Quận 1, TP.HCM", "TP.HCM", "1900 6017"),
                        Cinema("2", "CGV Aeon Mall Bình Tân", "Số 1 đường số 17A, Bình Trị Đông B, Bình Tân, TP.HCM", "TP.HCM", "1900 6017"),
                        Cinema("3", "CGV Crescent Mall", "Tầng 5, Crescent Mall, 101 Tôn Dật Tiên, Tân Phú, Quận 7, TP.HCM", "TP.HCM", "1900 6017")
                    )

                    // Thêm dữ liệu mẫu vào Firestore
                    for (cinema in dummyCinemas) {
                        db.collection("cinemas").document(cinema.id)
                            .set(cinema)
                            .addOnSuccessListener {
                                Log.d("MovieDetailActivity", "Cinema added: ${cinema.name}")
                            }
                            .addOnFailureListener { e ->
                                Log.e("MovieDetailActivity", "Error adding cinema: ${e.message}")
                            }
                    }

                    cinemas.addAll(dummyCinemas)
                }

                // Thiết lập spinner rạp
                if (cinemas.isNotEmpty()) {
                    selectedCinema = cinemas[0]

                    // Tạo adapter cho spinner
                    val adapter = android.widget.ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_item,
                        cinemas.map { it.name }
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerCinema.adapter = adapter

                    // Thiết lập sự kiện khi chọn rạp
                    spinnerCinema.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                            selectedCinema = cinemas[position]
                            loadShowtimes(selectedCinema.id, selectedDate)
                        }

                        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                            // Do nothing
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("MovieDetailActivity", "Error loading cinemas: ${e.message}")

                // Sử dụng dữ liệu mẫu nếu có lỗi
                val dummyCinemas = listOf(
                    Cinema("1", "CGV Vincom Center", "Số 72 Lê Thánh Tôn, Bến Nghé, Quận 1, TP.HCM", "TP.HCM", "1900 6017"),
                    Cinema("2", "CGV Aeon Mall Bình Tân", "Số 1 đường số 17A, Bình Trị Đông B, Bình Tân, TP.HCM", "TP.HCM", "1900 6017"),
                    Cinema("3", "CGV Crescent Mall", "Tầng 5, Crescent Mall, 101 Tôn Dật Tiên, Tân Phú, Quận 7, TP.HCM", "TP.HCM", "1900 6017")
                )

                selectedCinema = dummyCinemas[0]

                // Tạo adapter cho spinner
                val adapter = android.widget.ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    dummyCinemas.map { it.name }
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerCinema.adapter = adapter

                // Thiết lập sự kiện khi chọn rạp
                spinnerCinema.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                        selectedCinema = dummyCinemas[position]
                        loadShowtimes(selectedCinema.id, selectedDate)
                    }

                    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                        // Do nothing
                    }
                }
            }
    }

    private fun setupDates() {
        // Thiết lập ngày hiện tại là ngày được chọn
        selectedDate = Calendar.getInstance().time

        // Tạo danh sách ngày trong 7 ngày tới
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

        val layoutDates = findViewById<android.widget.LinearLayout>(R.id.layoutDates)
        layoutDates.removeAllViews()

        for (i in 0 until 7) {
            val date = calendar.time
            val dateView = layoutInflater.inflate(R.layout.item_date, layoutDates, false)

            val tvDay = dateView.findViewById<TextView>(R.id.tvDay)
            val tvDate = dateView.findViewById<TextView>(R.id.tvDate)

            tvDay.text = dayFormat.format(date).uppercase()
            tvDate.text = dateFormat.format(date)

            // Thiết lập sự kiện khi chọn ngày
            dateView.setOnClickListener {
                selectedDate = date
                loadShowtimes(selectedCinema.id, selectedDate)

                // Cập nhật UI để hiển thị ngày được chọn
                for (j in 0 until layoutDates.childCount) {
                    val child = layoutDates.getChildAt(j)
                    if (j == i) {
                        child.setBackgroundResource(R.color.cgv_red)
                        child.findViewById<TextView>(R.id.tvDay).setTextColor(resources.getColor(R.color.white))
                        child.findViewById<TextView>(R.id.tvDate).setTextColor(resources.getColor(R.color.white))
                    } else {
                        child.setBackgroundResource(R.drawable.rounded_corner_bg)
                        child.findViewById<TextView>(R.id.tvDay).setTextColor(resources.getColor(R.color.cgv_text_gray))
                        child.findViewById<TextView>(R.id.tvDate).setTextColor(resources.getColor(R.color.black))
                    }
                }
            }

            // Nếu là ngày đầu tiên, thiết lập là ngày được chọn
            if (i == 0) {
                dateView.setBackgroundResource(R.color.cgv_red)
                tvDay.setTextColor(resources.getColor(R.color.white))
                tvDate.setTextColor(resources.getColor(R.color.white))
            }

            layoutDates.addView(dateView)

            // Chuyển đến ngày tiếp theo
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Tải suất chiếu cho ngày và rạp được chọn
        if (::selectedCinema.isInitialized) {
            loadShowtimes(selectedCinema.id, selectedDate)
        }
    }

    private fun loadShowtimes(cinemaId: String, date: Date) {
        // Định dạng ngày để tìm kiếm suất chiếu
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateStr = dateFormat.format(date)

        // Tìm kiếm suất chiếu trong Firestore
        db.collection("showtimes")
            .whereEqualTo("movieId", movie.id)
            .whereEqualTo("cinemaId", cinemaId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Lọc các suất chiếu theo ngày
                    val showtimesList = documents.toObjects(Showtime::class.java)
                        .filter {
                            val showtimeDate = it.date
                            dateFormat.format(showtimeDate) == dateStr
                        }

                    if (showtimesList.isNotEmpty()) {
                        showtimes = showtimesList

                        // Cập nhật UI với danh sách suất chiếu
                        val adapter = com.example.myapplication.adapter.ShowtimeAdapter(showtimes) { showtime ->
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
                        return@addOnSuccessListener
                    }
                }

                // Nếu không tìm thấy suất chiếu trong Firestore, tạo suất chiếu mẫu
                setupShowtimes()

                // Sử dụng dữ liệu mẫu
                val dummyShowtimes = listOf(
                    Showtime("1", movie.id, cinemaId, date, "10:00", generateAvailableSeats(), listOf("A1", "A2"), 100000.0),
                    Showtime("2", movie.id, cinemaId, date, "13:30", generateAvailableSeats(), listOf("B5", "C7"), 100000.0),
                    Showtime("3", movie.id, cinemaId, date, "16:00", generateAvailableSeats(), listOf("D3", "D4"), 100000.0),
                    Showtime("4", movie.id, cinemaId, date, "19:30", generateAvailableSeats(), listOf("E8", "F2"), 120000.0),
                    Showtime("5", movie.id, cinemaId, date, "22:00", generateAvailableSeats(), listOf("G5", "H10"), 120000.0)
                )

                showtimes = dummyShowtimes

                // Cập nhật UI với danh sách suất chiếu mẫu
                val adapter = com.example.myapplication.adapter.ShowtimeAdapter(showtimes) { showtime ->
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
            .addOnFailureListener { e ->
                Log.e("MovieDetailActivity", "Error loading showtimes: ${e.message}")

                // Sử dụng dữ liệu mẫu nếu có lỗi
                val dummyShowtimes = listOf(
                    Showtime("1", movie.id, cinemaId, date, "10:00", generateAvailableSeats(), listOf("A1", "A2"), 100000.0),
                    Showtime("2", movie.id, cinemaId, date, "13:30", generateAvailableSeats(), listOf("B5", "C7"), 100000.0),
                    Showtime("3", movie.id, cinemaId, date, "16:00", generateAvailableSeats(), listOf("D3", "D4"), 100000.0),
                    Showtime("4", movie.id, cinemaId, date, "19:30", generateAvailableSeats(), listOf("E8", "F2"), 120000.0),
                    Showtime("5", movie.id, cinemaId, date, "22:00", generateAvailableSeats(), listOf("G5", "H10"), 120000.0)
                )

                showtimes = dummyShowtimes

                // Cập nhật UI với danh sách suất chiếu mẫu
                val adapter = com.example.myapplication.adapter.ShowtimeAdapter(showtimes) { showtime ->
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
    }

    // Thêm phương thức setupShowtimes
    private fun setupShowtimes() {
        // Tạo các suất chiếu mẫu nếu chưa có trong Firestore
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // Tạo danh sách tất cả các ghế
        val allSeats = generateAllSeats()

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
                    id = "showtime_${movie.id}_${dateStr}_1000",
                    movieId = movie.id,
                    cinemaId = "1",
                    date = date,
                    time = "10:00",
                    availableSeats = availableSeats,
                    bookedSeats = randomBookedSeats,
                    price = 100000.0
                ),
                Showtime(
                    id = "showtime_${movie.id}_${dateStr}_1330",
                    movieId = movie.id,
                    cinemaId = "1",
                    date = date,
                    time = "13:30",
                    availableSeats = availableSeats,
                    bookedSeats = randomBookedSeats,
                    price = 100000.0
                ),
                Showtime(
                    id = "showtime_${movie.id}_${dateStr}_1600",
                    movieId = movie.id,
                    cinemaId = "1",
                    date = date,
                    time = "16:00",
                    availableSeats = availableSeats,
                    bookedSeats = randomBookedSeats,
                    price = 100000.0
                ),
                Showtime(
                    id = "showtime_${movie.id}_${dateStr}_1930",
                    movieId = movie.id,
                    cinemaId = "1",
                    date = date,
                    time = "19:30",
                    availableSeats = availableSeats,
                    bookedSeats = randomBookedSeats,
                    price = 120000.0
                ),
                Showtime(
                    id = "showtime_${movie.id}_${dateStr}_2200",
                    movieId = movie.id,
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

    // Thêm phương thức để tạo danh sách tất cả các ghế
    private fun generateAllSeats(): List<String> {
        val allSeats = mutableListOf<String>()
        val rows = 8
        val cols = 10

        for (i in 0 until rows) {
            val rowChar = ('A' + i).toString()
            for (j in 1..cols) {
                allSeats.add("$rowChar$j")
            }
        }

        return allSeats
    }

    // Thêm phương thức để tạo danh sách ghế còn trống
    private fun generateAvailableSeats(): List<String> {
        val allSeats = generateAllSeats()
        val bookedSeats = listOf("A1", "A2", "B5", "C7", "D3", "D4", "E8", "F2", "G5", "H10")
        return allSeats.filter { !bookedSeats.contains(it) }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
   }
}