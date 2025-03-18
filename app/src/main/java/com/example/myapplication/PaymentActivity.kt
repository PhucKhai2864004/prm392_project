package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.model.Booking
import com.example.myapplication.model.Showtime
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

    private lateinit var tvMovieTitle: TextView
    private lateinit var tvCinema: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvSeats: TextView
    private lateinit var tvTicketPrice: TextView
    private lateinit var tvConvenienceFee: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var radioGroupPayment: RadioGroup
    private lateinit var layoutCardDetails: LinearLayout
    private lateinit var layoutVNPay: LinearLayout
    private lateinit var btnConfirmPayment: Button

    private lateinit var movieId: String
    private lateinit var movieTitle: String
    private lateinit var cinemaId: String
    private lateinit var cinemaName: String
    private lateinit var showtimeId: String
    private lateinit var showDate: String
    private lateinit var showTime: String
    private lateinit var selectedSeats: Array<String>
    private var totalAmount: Double = 0.0
    private var seatPrice: Double = 100000.0 // Giá vé mặc định

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        // Thiết lập toolbar đúng cách
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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
        tvMovieTitle = findViewById(R.id.tvMovieTitle)
        tvCinema = findViewById(R.id.tvCinema)
        tvDate = findViewById(R.id.tvDate)
        tvTime = findViewById(R.id.tvTime)
        tvSeats = findViewById(R.id.tvSeats)
        tvTicketPrice = findViewById(R.id.tvTicketPrice)
        tvConvenienceFee = findViewById(R.id.tvConvenienceFee)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        radioGroupPayment = findViewById(R.id.radioGroupPayment)
        layoutCardDetails = findViewById(R.id.layoutCardDetails)
        layoutVNPay = findViewById(R.id.layoutVNPay)
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment)

        // Get data from intent
        movieId = intent.getStringExtra("MOVIE_ID") ?: ""
        movieTitle = intent.getStringExtra("MOVIE_TITLE") ?: ""
        cinemaId = intent.getStringExtra("CINEMA_ID") ?: ""
        cinemaName = intent.getStringExtra("CINEMA_NAME") ?: ""
        showtimeId = intent.getStringExtra("SHOWTIME_ID") ?: ""
        showDate = intent.getStringExtra("SHOW_DATE") ?: ""
        showTime = intent.getStringExtra("SHOW_TIME") ?: ""
        selectedSeats = intent.getStringArrayExtra("SELECTED_SEATS") ?: arrayOf()
        totalAmount = intent.getDoubleExtra("TOTAL_AMOUNT", 0.0)

        // Lấy giá vé từ Firestore
        db.collection("showtimes").document(showtimeId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val showtime = document.toObject(Showtime::class.java)
                    showtime?.let {
                        seatPrice = it.price
                        updateUI()
                    }
                } else {
                    updateUI()
                }
            }
            .addOnFailureListener { e ->
                updateUI()
            }

        // Set up payment method selection
        radioGroupPayment.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbCreditCard -> {
                    layoutCardDetails.visibility = View.VISIBLE
                    layoutVNPay.visibility = View.GONE
                }
                else -> {
                    layoutCardDetails.visibility = View.GONE
                    layoutVNPay.visibility = View.VISIBLE
                }
            }
        }

        // Set up confirm payment button
        btnConfirmPayment.setOnClickListener {
            processPayment()
        }
    }

    private fun updateUI() {
        tvMovieTitle.text = movieTitle
        tvCinema.text = cinemaName
        tvDate.text = showDate
        tvTime.text = showTime
        tvSeats.text = selectedSeats.joinToString(", ")

        // Định dạng tiền tệ VND
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))

        // Hiển thị giá vé
        tvTicketPrice.text = "${formatter.format(seatPrice)} VND x ${selectedSeats.size}"

        // Phí dịch vụ
        val convenienceFee = 15000.0
        tvConvenienceFee.text = "${formatter.format(convenienceFee)} VND"

        // Tổng tiền
        totalAmount = (seatPrice * selectedSeats.size) + convenienceFee
        tvTotalAmount.text = "${formatter.format(totalAmount)} VND"
    }

    private fun getSelectedPaymentMethod(): String {
        return when (radioGroupPayment.checkedRadioButtonId) {
            R.id.rbVNPay -> "VNPay"
            R.id.rbCreditCard -> "Credit Card"
            R.id.rbMomo -> "MoMo"
            R.id.rbZaloPay -> "ZaloPay"
            else -> "VNPay"
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

        // Cập nhật danh sách ghế đã đặt trong Firestore
        val showtimeRef = db.collection("showtimes").document(showtimeId)

        db.runTransaction { transaction ->
            // Lấy dữ liệu showtime hiện tại
            val snapshot = transaction.get(showtimeRef)
            val showtime = snapshot.toObject(Showtime::class.java)

            if (showtime != null) {
                // Tạo danh sách ghế đã đặt mới
                val updatedBookedSeats = ArrayList(showtime.bookedSeats)
                updatedBookedSeats.addAll(selectedSeats)

                // Tạo danh sách ghế còn trống mới
                val updatedAvailableSeats = ArrayList(showtime.availableSeats)
                updatedAvailableSeats.removeAll(selectedSeats.toSet())

                // Cập nhật Showtime trong transaction
                transaction.update(showtimeRef, "bookedSeats", updatedBookedSeats)
                transaction.update(showtimeRef, "availableSeats", updatedAvailableSeats)
            } else {
                // Nếu không tìm thấy showtime, tạo mới
                val newShowtime = Showtime(
                    id = showtimeId,
                    movieId = movieId,
                    cinemaId = cinemaId,
                    date = showDateObj,
                    time = showTime,
                    bookedSeats = selectedSeats.toList(),
                    availableSeats = generateAllSeats().filter { !selectedSeats.contains(it) },
                    price = seatPrice
                )
                transaction.set(showtimeRef, newShowtime)
            }

            // Lưu booking trong cùng transaction
            val bookingRef = db.collection("bookings").document(bookingId)
            transaction.set(bookingRef, booking)

            // Trả về null để hoàn thành transaction
            null
        }.addOnSuccessListener {
            // Transaction thành công
            // Chuyển đến màn hình xác nhận đặt vé
            val intent = Intent(this, BookingConfirmationActivity::class.java)
            intent.putExtra("BOOKING_ID", bookingId)
            // Thêm flag để xóa tất cả các activity trước đó trong stack
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }.addOnFailureListener { e ->
            // Transaction thất bại
            Toast.makeText(this, "Lỗi khi xử lý đặt vé: ${e.message}", Toast.LENGTH_SHORT).show()
            findViewById<Button>(R.id.btnConfirmPayment).isEnabled = true
            findViewById<Button>(R.id.btnConfirmPayment).text = "Xác nhận thanh toán"
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}