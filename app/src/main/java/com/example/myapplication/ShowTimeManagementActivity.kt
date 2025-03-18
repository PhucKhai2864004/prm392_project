package com.example.myapplication

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.model.Cinema
import com.example.myapplication.model.Showtime
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class ShowtimeManagementActivity : AppCompatActivity() {

    private val db = Firebase.firestore

    private lateinit var tvMovieTitle: TextView
    private lateinit var rvShowtimes: RecyclerView
    private lateinit var tvNoShowtimes: TextView
    private lateinit var fabAddShowtime: FloatingActionButton

    private lateinit var movieId: String
    private lateinit var movieTitle: String
    private val showtimes = mutableListOf<Showtime>()
    private val cinemas = mutableListOf<Cinema>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_showtime_management)

        // Thiết lập toolbar đúng cách
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Lấy dữ liệu từ intent
        movieId = intent.getStringExtra("MOVIE_ID") ?: ""
        movieTitle = intent.getStringExtra("MOVIE_TITLE") ?: ""

        if (movieId.isEmpty() || movieTitle.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin phim", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Khởi tạo views
        tvMovieTitle = findViewById(R.id.tvMovieTitle)
        rvShowtimes = findViewById(R.id.rvShowtimes)
        tvNoShowtimes = findViewById(R.id.tvNoShowtimes)
        fabAddShowtime = findViewById(R.id.fabAddShowtime)

        // Cập nhật tiêu đề
        tvMovieTitle.text = movieTitle

        // Thiết lập sự kiện click cho nút thêm lịch chiếu
        fabAddShowtime.setOnClickListener {
            showAddEditShowtimeDialog(null)
        }

        // Tải danh sách rạp
        loadCinemas()

        // Tải danh sách lịch chiếu
        loadShowtimes()
    }

    private fun loadCinemas() {
        db.collection("cinemas")
            .get()
            .addOnSuccessListener { documents ->
                cinemas.clear()
                for (document in documents) {
                    val cinema = document.toObject(Cinema::class.java)
                    cinemas.add(cinema)
                }

                if (cinemas.isEmpty()) {
                    // Hiển thị thông báo nếu không có rạp phim
                    Toast.makeText(this, "Không có rạp phim nào", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("ShowtimeManagementActivity", "Error loading cinemas: ${e.message}")
                Toast.makeText(this, "Lỗi khi tải danh sách rạp: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadShowtimes() {
        db.collection("showtimes")
            .whereEqualTo("movieId", movieId)
            .get()
            .addOnSuccessListener { documents ->
                showtimes.clear()
                for (document in documents) {
                    val showtime = document.toObject(Showtime::class.java)
                    showtimes.add(showtime)
                }

                if (showtimes.isEmpty()) {
                    tvNoShowtimes.visibility = View.VISIBLE
                    rvShowtimes.visibility = View.GONE
                } else {
                    tvNoShowtimes.visibility = View.GONE
                    rvShowtimes.visibility = View.VISIBLE

                    // Sắp xếp lịch chiếu theo ngày và giờ
                    showtimes.sortWith(compareBy<Showtime> { it.date }.thenBy { it.time })

                    // Hiển thị danh sách lịch chiếu
                    val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                        inner class ShowtimeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
                            val tvCinemaName: TextView = itemView.findViewById(R.id.tvCinemaName)
                            val tvDateTime: TextView = itemView.findViewById(R.id.tvDateTime)
                            val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
                            val tvSeatsInfo: TextView = itemView.findViewById(R.id.tvSeatsInfo)
                            val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
                            val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
                        }

                        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                            val view = LayoutInflater.from(parent.context)
                                .inflate(R.layout.item_admin_showtime, parent, false)
                            return ShowtimeViewHolder(view)
                        }

                        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                            val showtime = showtimes[position]
                            val viewHolder = holder as ShowtimeViewHolder

                            // Tìm tên rạp
                            val cinema = cinemas.find { it.id == showtime.cinemaId }
                            val cinemaName = cinema?.name ?: "Rạp không xác định"

                            viewHolder.tvCinemaName.text = cinemaName

                            // Định dạng ngày và giờ
                            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            val dateStr = dateFormat.format(showtime.date)
                            viewHolder.tvDateTime.text = "$dateStr | ${showtime.time}"

                            // Định dạng giá vé
                            val formatter = java.text.NumberFormat.getNumberInstance(Locale("vi", "VN"))
                            viewHolder.tvPrice.text = "${formatter.format(showtime.price)} VND"

                            // Thông tin ghế
                            val availableSeats = showtime.availableSeats.size
                            val bookedSeats = showtime.bookedSeats.size
                            viewHolder.tvSeatsInfo.text = "Còn trống: $availableSeats | Đã đặt: $bookedSeats"

                            // Thiết lập sự kiện click cho nút sửa
                            viewHolder.btnEdit.setOnClickListener {
                                showAddEditShowtimeDialog(showtime)
                            }

                            // Thiết lập sự kiện click cho nút xóa
                            viewHolder.btnDelete.setOnClickListener {
                                showDeleteShowtimeDialog(showtime)
                            }
                        }

                        override fun getItemCount(): Int = showtimes.size
                    }

                    rvShowtimes.adapter = adapter
                }
            }
            .addOnFailureListener { e ->
                Log.e("ShowtimeManagementActivity", "Error loading showtimes: ${e.message}")
                Toast.makeText(this, "Lỗi khi tải danh sách lịch chiếu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAddEditShowtimeDialog(showtime: Showtime?) {
        val isEdit = showtime != null
        val dialogTitle = if (isEdit) "Chỉnh sửa lịch chiếu" else "Thêm lịch chiếu mới"

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_showtime, null)
        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val spinnerCinema = dialogView.findViewById<Spinner>(R.id.spinnerCinema)
        val etDate = dialogView.findViewById<EditText>(R.id.etDate)
        val etTime = dialogView.findViewById<EditText>(R.id.etTime)
        val etPrice = dialogView.findViewById<EditText>(R.id.etPrice)
        val btnSelectDate = dialogView.findViewById<Button>(R.id.btnSelectDate)
        val btnSelectTime = dialogView.findViewById<Button>(R.id.btnSelectTime)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        tvDialogTitle.text = dialogTitle

        // Thiết lập spinner rạp
        val cinemaNames = cinemas.map { it.name }
        val cinemaAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cinemaNames)
        cinemaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCinema.adapter = cinemaAdapter

        // Nếu là chỉnh sửa, điền thông tin lịch chiếu vào form
        var selectedCinemaPosition = 0
        if (isEdit) {
            // Tìm vị trí của rạp trong danh sách
            selectedCinemaPosition = cinemas.indexOfFirst { it.id == showtime!!.cinemaId }
            if (selectedCinemaPosition != -1) {
                spinnerCinema.setSelection(selectedCinemaPosition)
            }

            // Định dạng ngày
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            etDate.setText(dateFormat.format(showtime!!.date))

            // Giờ chiếu
            etTime.setText(showtime.time)

            // Giá vé
            etPrice.setText(showtime.price.toString())
        } else {
            // Mặc định giá vé là 100,000 VND
            etPrice.setText("100000")
        }

        // Thiết lập sự kiện chọn ngày
        btnSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance()

            // Nếu đã có ngày, sử dụng ngày đó
            if (etDate.text.isNotEmpty()) {
                try {
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val date = dateFormat.parse(etDate.text.toString())
                    if (date != null) {
                        calendar.time = date
                    }
                } catch (e: Exception) {
                    // Nếu có lỗi, sử dụng ngày hiện tại
                }
            }

            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                    etDate.setText(selectedDate)
                },
                year,
                month,
                day
            )

            datePickerDialog.show()
        }

        // Thiết lập sự kiện chọn giờ
        btnSelectTime.setOnClickListener {
            val calendar = Calendar.getInstance()

            // Nếu đã có giờ, sử dụng giờ đó
            if (etTime.text.isNotEmpty()) {
                try {
                    val timeParts = etTime.text.toString().split(":")
                    if (timeParts.size == 2) {
                        calendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                        calendar.set(Calendar.MINUTE, timeParts[1].toInt())
                    }
                } catch (e: Exception) {
                    // Nếu có lỗi, sử dụng giờ hiện tại
                }
            }

            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(
                this,
                { _, selectedHour, selectedMinute ->
                    val selectedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                    etTime.setText(selectedTime)
                },
                hour,
                minute,
                true
            )

            timePickerDialog.show()
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            // Lấy rạp đã chọn
            val selectedCinemaPosition = spinnerCinema.selectedItemPosition
            if (selectedCinemaPosition == -1 || selectedCinemaPosition >= cinemas.size) {
                Toast.makeText(this, "Vui lòng chọn rạp", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedCinema = cinemas[selectedCinemaPosition]

            // Kiểm tra ngày
            val dateStr = etDate.text.toString().trim()
            if (dateStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ngày", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Kiểm tra giờ
            val timeStr = etTime.text.toString().trim()
            if (timeStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn giờ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Kiểm tra giá vé
            val priceStr = etPrice.text.toString().trim()
            if (priceStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập giá vé", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Chuyển đổi ngày
            var date: Date? = null
            try {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                date = dateFormat.parse(dateStr)
            } catch (e: Exception) {
                Toast.makeText(this, "Định dạng ngày không hợp lệ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (date == null) {
                Toast.makeText(this, "Ngày không hợp lệ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Chuyển đổi giá vé
            val price: Double
            try {
                price = priceStr.toDouble()
            } catch (e: Exception) {
                Toast.makeText(this, "Giá vé không hợp lệ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Tạo ID cho lịch chiếu
            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val dateId = dateFormat.format(date)
            val timeId = timeStr.replace(":", "")
            val showtimeId = showtime?.id ?: "showtime_${movieId}_${selectedCinema.id}_${dateId}_$timeId"

            // Tạo hoặc cập nhật lịch chiếu
            val updatedShowtime: Showtime

            if (isEdit) {
                // Cập nhật lịch chiếu hiện có
                updatedShowtime = showtime!!.copy(
                    cinemaId = selectedCinema.id,
                    date = date,
                    time = timeStr,
                    price = price
                )
            } else {
                // Tạo lịch chiếu mới
                updatedShowtime = Showtime(
                    id = showtimeId,
                    movieId = movieId,
                    cinemaId = selectedCinema.id,
                    date = date,
                    time = timeStr,
                    availableSeats = generateAllSeats(),
                    bookedSeats = listOf(),
                    price = price
                )
            }

            saveShowtime(updatedShowtime, isEdit)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveShowtime(showtime: Showtime, isEdit: Boolean) {
        db.collection("showtimes").document(showtime.id)
            .set(showtime)
            .addOnSuccessListener {
                val message = if (isEdit) "Lịch chiếu đã được cập nhật" else "Lịch chiếu đã được thêm"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                loadShowtimes()
            }
            .addOnFailureListener { e ->
                Log.e("ShowtimeManagementActivity", "Error saving showtime: ${e.message}")
                Toast.makeText(this, "Lỗi khi lưu lịch chiếu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteShowtimeDialog(showtime: Showtime) {
        // Tìm tên rạp
        val cinema = cinemas.find { it.id == showtime.cinemaId }
        val cinemaName = cinema?.name ?: "Rạp không xác định"

        // Định dạng ngày
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateStr = dateFormat.format(showtime.date)

        AlertDialog.Builder(this)
            .setTitle("Xóa lịch chiếu")
            .setMessage("Bạn có chắc chắn muốn xóa lịch chiếu này?\n\nRạp: $cinemaName\nNgày: $dateStr\nGiờ: ${showtime.time}")
            .setPositiveButton("Xóa") { _, _ ->
                deleteShowtime(showtime)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteShowtime(showtime: Showtime) {
        db.collection("showtimes").document(showtime.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Lịch chiếu đã được xóa", Toast.LENGTH_SHORT).show()
                loadShowtimes()
            }
            .addOnFailureListener { e ->
                Log.e("ShowtimeManagementActivity", "Error deleting showtime: ${e.message}")
                Toast.makeText(this, "Lỗi khi xóa lịch chiếu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

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

