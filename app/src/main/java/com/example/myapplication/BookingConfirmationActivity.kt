package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.model.Booking
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class BookingConfirmationActivity : AppCompatActivity() {

    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_confirmation)

        // Thiết lập toolbar đúng cách
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Lấy ID đơn đặt vé từ intent
        val bookingId = intent.getStringExtra("BOOKING_ID") ?: ""
        if (bookingId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy đơn đặt vé", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Tải thông tin đơn đặt vé
        loadBookingDetails(bookingId)

        // Thiết lập nút hoàn tất
        findViewById<Button>(R.id.btnDone).setOnClickListener {
            // Chuyển về MainActivity và xóa tất cả các activity trước đó
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
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
                    Toast.makeText(this, "Không tìm thấy đơn đặt vé", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi khi tải đơn đặt vé: ${e.message}", Toast.LENGTH_SHORT).show()
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

        // Trong ứng dụng thực tế, bạn sẽ tạo mã QR cho đơn đặt vé
        // Ví dụ:
        // val qrCode = generateQRCode(booking.id)
        // findViewById<ImageView>(R.id.ivQrCode).setImageBitmap(qrCode)
    }

    // Ghi đè phương thức onBackPressed để ngăn người dùng quay lại màn hình thanh toán
//    override fun onBackPressed() {
//        // Chuyển về MainActivity thay vì quay lại màn hình trước đó
//        val intent = Intent(this, MainActivity::class.java)
//        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
//        startActivity(intent)
//        finish()
//    }

    override fun onSupportNavigateUp(): Boolean {
        // Xử lý khi người dùng nhấn nút back trên toolbar
        onBackPressed()
        return true
    }
}