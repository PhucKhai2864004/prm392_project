package com.example.myapplication

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.model.Booking
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class TicketDetailActivity : AppCompatActivity() {

    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket_detail)

        // Thiết lập toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Lấy ID đơn đặt vé từ intent
        val bookingId = intent.getStringExtra("BOOKING_ID") ?: ""
        if (bookingId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin vé", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Tải thông tin đơn đặt vé
        loadBookingDetails(bookingId)
    }

    private fun loadBookingDetails(bookingId: String) {
        db.collection("bookings").document(bookingId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val booking = document.toObject(Booking::class.java)
                    booking?.let {
                        updateUI(it)
                    }
                } else {
                    Toast.makeText(this, "Không tìm thấy thông tin vé", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi khi tải thông tin vé: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun updateUI(booking: Booking) {
        findViewById<TextView>(R.id.tvBookingId).text = booking.id
        findViewById<TextView>(R.id.tvMovieTitle).text = booking.movieTitle
        findViewById<TextView>(R.id.tvCinema).text = booking.cinemaName

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        findViewById<TextView>(R.id.tvDate).text = dateFormat.format(booking.showDate)

        findViewById<TextView>(R.id.tvTime).text = booking.showTime
        findViewById<TextView>(R.id.tvSeats).text = booking.seats.joinToString(", ")

        // Định dạng tiền tệ VND
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        findViewById<TextView>(R.id.tvAmountPaid).text = "${formatter.format(booking.totalAmount)} VND"

        // Hiển thị trạng thái
        findViewById<TextView>(R.id.tvStatus).text = booking.status
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

