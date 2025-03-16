package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.model.Booking
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PaymentActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    private lateinit var movieId: String
    private lateinit var movieTitle: String
    private lateinit var cinemaId: String
    private lateinit var cinemaName: String
    private lateinit var showtimeId: String
    private lateinit var showDate: String
    private lateinit var showTime: String
    private lateinit var selectedSeats: Array<String>
    private var totalAmount: Double = 0.0

    private lateinit var layoutCardDetails: LinearLayout
    private lateinit var layoutVNPay: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        // Thiết lập toolbar đúng cách
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Get data from intent
        movieId = intent.getStringExtra("MOVIE_ID") ?: ""
        movieTitle = intent.getStringExtra("MOVIE_TITLE") ?: ""
        cinemaId = intent.getStringExtra("CINEMA_ID") ?: ""
        cinemaName = intent.getStringExtra("CINEMA_NAME") ?: ""
        showtimeId = intent.getStringExtra("SHOWTIME_ID") ?: ""
        showDate = intent.getStringExtra("SHOW_DATE") ?: ""
        showTime = intent.getStringExtra("SHOW_TIME") ?: ""
        selectedSeats = intent.getStringArrayExtra("SELECTED_SEATS") ?: emptyArray()
        totalAmount = intent.getDoubleExtra("TOTAL_AMOUNT", 0.0)

        // Khởi tạo views
        layoutCardDetails = findViewById(R.id.layoutCardDetails)
        layoutVNPay = findViewById(R.id.layoutVNPay)

        // Cập nhật thông tin đặt vé
        findViewById<TextView>(R.id.tvMovieTitle).text = movieTitle
        findViewById<TextView>(R.id.tvCinema).text = cinemaName
        findViewById<TextView>(R.id.tvDate).text = showDate
        findViewById<TextView>(R.id.tvTime).text = showTime

        findViewById<TextView>(R.id.tvSeats).text = selectedSeats.joinToString(", ")

        // Định dạng tiền tệ VND
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))

        // Cập nhật giá vé và phí dịch vụ
        val seatPrice = 100000.0 // 100,000 VND
        val convenienceFee = 15000.0 // 15,000 VND

        findViewById<TextView>(R.id.tvTicketPrice).text = "${formatter.format(seatPrice)} VND x ${selectedSeats.size}"
        findViewById<TextView>(R.id.tvConvenienceFee).text = "${formatter.format(convenienceFee)} VND"
        findViewById<TextView>(R.id.tvTotalAmount).text = "${formatter.format(totalAmount + convenienceFee)} VND"

        // Thiết lập lựa chọn phương thức thanh toán
        val radioGroupPayment = findViewById<RadioGroup>(R.id.radioGroupPayment)

        radioGroupPayment.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbVNPay -> {
                    layoutVNPay.visibility = View.VISIBLE
                    layoutCardDetails.visibility = View.GONE
                }
                R.id.rbCreditCard -> {
                    layoutVNPay.visibility = View.GONE
                    layoutCardDetails.visibility = View.VISIBLE
                }
                else -> {
                    layoutVNPay.visibility = View.GONE
                    layoutCardDetails.visibility = View.GONE
                }
            }
        }

        // Thiết lập nút xác nhận thanh toán
        findViewById<Button>(R.id.btnConfirmPayment).setOnClickListener {
            processPayment()
        }
    }

    private fun processPayment() {
        // Trong ứng dụng thực tế, bạn sẽ tích hợp với cổng thanh toán VNPay
        // Ở đây, chúng ta chỉ giả lập quá trình thanh toán

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Người dùng chưa đăng nhập", Toast.LENGTH_SHORT).show()
            return
        }

        // Hiển thị trạng thái đang xử lý
        findViewById<Button>(R.id.btnConfirmPayment).isEnabled = false
        findViewById<Button>(R.id.btnConfirmPayment).text = "Đang xử lý..."

        // Tạo đơn đặt vé
        val bookingId = UUID.randomUUID().toString()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val showDateObj = dateFormat.parse(showDate) ?: Date()

        // Phí dịch vụ
        val convenienceFee = 15000.0

        val booking = Booking(
            id = bookingId,
            userId = userId,
            movieId = movieId,
            movieTitle = movieTitle,
            cinemaId = cinemaId,
            cinemaName = cinemaName,
            showDate = showDateObj,
            showTime = showTime,
            seats = selectedSeats.toList(),
            totalAmount = totalAmount + convenienceFee,
            paymentMethod = getSelectedPaymentMethod(),
            bookingDate = Date(),
            status = "Đã xác nhận"
        )

        // Lưu đơn đặt vé vào Firestore
        db.collection("bookings").document(bookingId)
            .set(booking)
            .addOnSuccessListener {
                // Chuyển đến màn hình xác nhận đặt vé
                val intent = Intent(this, BookingConfirmationActivity::class.java)
                intent.putExtra("BOOKING_ID", bookingId)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi khi tạo đơn đặt vé: ${e.message}", Toast.LENGTH_SHORT).show()
                findViewById<Button>(R.id.btnConfirmPayment).isEnabled = true
                findViewById<Button>(R.id.btnConfirmPayment).text = "Xác nhận thanh toán"
            }
    }

    private fun getSelectedPaymentMethod(): String {
        val radioGroupPayment = findViewById<RadioGroup>(R.id.radioGroupPayment)
        return when (radioGroupPayment.checkedRadioButtonId) {
            R.id.rbVNPay -> "VNPay"
            R.id.rbCreditCard -> "Thẻ tín dụng/ghi nợ"
            R.id.rbMomo -> "Ví MoMo"
            R.id.rbZaloPay -> "ZaloPay"
            else -> "VNPay"
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}