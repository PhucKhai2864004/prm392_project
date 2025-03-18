package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.myapplication.model.Showtime
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.NumberFormat
import java.util.Locale

class SeatSelectionActivity : AppCompatActivity() {

    private val db = Firebase.firestore

    private lateinit var gridSeats: GridLayout
    private lateinit var tvSelectedSeats: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var btnProceed: Button

    private val selectedSeats = mutableListOf<String>()
    private var seatPrice = 100000.0  // Giá vé mặc định 100,000 VND

    private lateinit var movieId: String
    private lateinit var movieTitle: String
    private lateinit var cinemaId: String
    private lateinit var cinemaName: String
    private lateinit var showtimeId: String
    private lateinit var showDate: String
    private lateinit var showTime: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seat_selection)

        // Thiết lập toolbar đúng cách
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Get data from intent
        movieId = intent.getStringExtra("MOVIE_ID") ?: ""
        movieTitle = intent.getStringExtra("MOVIE_TITLE") ?: ""
        cinemaId = intent.getStringExtra("CINEMA_ID") ?: ""
        cinemaName = intent.getStringExtra("CINEMA_NAME") ?: ""
        showtimeId = intent.getStringExtra("SHOWTIME_ID") ?: ""
        showDate = intent.getStringExtra("SHOW_DATE") ?: ""
        showTime = intent.getStringExtra("SHOW_TIME") ?: ""

        // Initialize views
        gridSeats = findViewById(R.id.gridSeats)
        tvSelectedSeats = findViewById(R.id.tvSelectedSeats)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        btnProceed = findViewById(R.id.btnProceed)

        // Update movie info
        findViewById<TextView>(R.id.tvMovieTitle).text = movieTitle
        findViewById<TextView>(R.id.tvShowInfo).text = "$cinemaName | $showDate | $showTime"

        // Generate seats
        loadSeatsFromFirestore()

        // Set up proceed button
        btnProceed.setOnClickListener {
            if (selectedSeats.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất một ghế", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, PaymentActivity::class.java)
            intent.putExtra("MOVIE_ID", movieId)
            intent.putExtra("MOVIE_TITLE", movieTitle)
            intent.putExtra("CINEMA_ID", cinemaId)
            intent.putExtra("CINEMA_NAME", cinemaName)
            intent.putExtra("SHOWTIME_ID", showtimeId)
            intent.putExtra("SHOW_DATE", showDate)
            intent.putExtra("SHOW_TIME", showTime)
            intent.putExtra("SELECTED_SEATS", selectedSeats.toTypedArray())
            intent.putExtra("TOTAL_AMOUNT", selectedSeats.size * seatPrice)
            startActivity(intent)
        }
    }

    // Thêm onResume để tải lại dữ liệu mỗi khi activity được hiển thị
    override fun onResume() {
        super.onResume()
        // Tải lại dữ liệu ghế mỗi khi activity được hiển thị lại
        loadSeatsFromFirestore()
    }

    private fun loadSeatsFromFirestore() {
        // Xóa tất cả ghế hiện tại
        gridSeats.removeAllViews()
        selectedSeats.clear()
        tvSelectedSeats.text = "Chưa chọn"
        tvTotalAmount.text = "0 VND"

        // Tải dữ liệu từ Firestore
        db.collection("showtimes").document(showtimeId)
            .get()
            .addOnSuccessListener { document ->
                var bookedSeats = listOf<String>()

                if (document != null && document.exists()) {
                    val showtime = document.toObject(Showtime::class.java)
                    showtime?.let {
                        bookedSeats = it.bookedSeats
                        seatPrice = it.price
                        Log.d("SeatSelection", "Loaded booked seats: $bookedSeats")
                    }
                } else {
                    Log.d("SeatSelection", "No showtime found, using default booked seats")
                    // Nếu không tìm thấy suất chiếu, sử dụng danh s��ch ghế đã đặt mặc định
                    bookedSeats = listOf("A1", "A2", "B5", "C7", "D3", "D4", "E8", "F2", "G5", "H10")
                }

                // Tạo ghế
                generateSeats(bookedSeats)
            }
            .addOnFailureListener { e ->
                Log.e("SeatSelection", "Error loading showtime: ${e.message}")
                // Nếu có lỗi, sử dụng danh sách ghế đã đặt mặc định
                val bookedSeats = listOf("A1", "A2", "B5", "C7", "D3", "D4", "E8", "F2", "G5", "H10")
                generateSeats(bookedSeats)
            }
    }

    private fun generateSeats(bookedSeats: List<String>) {
        val rows = 8
        val cols = 10
        val seatSize = resources.getDimensionPixelSize(R.dimen.seat_size)
        val seatMargin = resources.getDimensionPixelSize(R.dimen.seat_margin)

        // Đặt số cột cho GridLayout
        gridSeats.columnCount = cols + 1  // +1 cho cột nhãn hàng
        gridSeats.rowCount = rows  // Đặt số hàng

        // Tạo ghế
        for (i in 0 until rows) {
            val rowChar = ('A' + i).toString()

            // Add row label
            val rowLabel = TextView(this)
            rowLabel.text = rowChar
            rowLabel.gravity = Gravity.CENTER
            rowLabel.setTextColor(ContextCompat.getColor(this, R.color.cgv_text_gray))

            val rowLabelParams = GridLayout.LayoutParams()
            rowLabelParams.width = seatSize
            rowLabelParams.height = seatSize
            rowLabelParams.setMargins(seatMargin, seatMargin, seatMargin, seatMargin)
            rowLabelParams.rowSpec = GridLayout.spec(i)
            rowLabelParams.columnSpec = GridLayout.spec(0)

            gridSeats.addView(rowLabel, rowLabelParams)

            for (j in 1..cols) {
                val seatId = "$rowChar$j"
                val isBooked = bookedSeats.contains(seatId)

                val seat = Button(this)
                seat.text = j.toString()
                seat.setTextColor(ContextCompat.getColor(this, R.color.white))
                seat.gravity = Gravity.CENTER

                if (isBooked) {
                    seat.setBackgroundColor(ContextCompat.getColor(this, R.color.cgv_gray))
                    seat.isEnabled = false
                } else {
                    seat.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
                    seat.setOnClickListener {
                        toggleSeatSelection(seat, seatId)
                    }
                }

                val seatParams = GridLayout.LayoutParams()
                seatParams.width = seatSize
                seatParams.height = seatSize
                seatParams.setMargins(seatMargin, seatMargin, seatMargin, seatMargin)
                seatParams.rowSpec = GridLayout.spec(i)
                seatParams.columnSpec = GridLayout.spec(j)

                gridSeats.addView(seat, seatParams)
            }
        }
    }

    private fun toggleSeatSelection(seat: Button, seatId: String) {
        if (selectedSeats.contains(seatId)) {
            // Deselect seat
            selectedSeats.remove(seatId)
            seat.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
        } else {
            // Select seat
            selectedSeats.add(seatId)
            seat.setBackgroundColor(ContextCompat.getColor(this, R.color.cgv_red))
        }

        // Update selected seats text
        if (selectedSeats.isEmpty()) {
            tvSelectedSeats.text = "Chưa chọn"
        } else {
            tvSelectedSeats.text = selectedSeats.sorted().joinToString(", ")
        }

        // Update total amount with VND format
        val totalAmount = selectedSeats.size * seatPrice
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        tvTotalAmount.text = "${formatter.format(totalAmount)} VND"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

