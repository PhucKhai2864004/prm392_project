package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.model.Cinema
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CinemaListActivity : AppCompatActivity() {

    private val db = Firebase.firestore

    private lateinit var rvCinemas: RecyclerView
    private lateinit var tvNoCinemas: TextView
    private lateinit var etSearch: EditText
    private lateinit var bottomNavigation: BottomNavigationView

    private var allCinemas = listOf<Cinema>()
    private var filteredCinemas = listOf<Cinema>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cinema_list)

        // Thiết lập toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Khởi tạo views
        rvCinemas = findViewById(R.id.rvCinemas)
        tvNoCinemas = findViewById(R.id.tvNoCinemas)
        etSearch = findViewById(R.id.etSearch)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        // Thiết lập sự kiện cho thanh tìm kiếm
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                filterCinemas(s.toString())
            }
        })

        // Thiết lập bottom navigation
        setupBottomNavigation()

        // Tải danh sách rạp phim
        loadCinemas()
    }

    private fun setupBottomNavigation() {
        // Đánh dấu mục đang chọn
        bottomNavigation.selectedItemId = R.id.nav_cinemas

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_cinemas -> {
                    // Đã ở trang rạp phim, không cần chuyển
                    true
                }
                R.id.nav_tickets -> {
                    startActivity(Intent(this, MyTicketsActivity::class.java))
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

    // Các phương thức khác giữ nguyên
    private fun loadCinemas() {
        db.collection("cinemas")
            .get()
            .addOnSuccessListener { documents ->
                allCinemas = documents.toObjects(Cinema::class.java)
                filteredCinemas = allCinemas

                updateCinemaList()
            }
            .addOnFailureListener { e ->
                tvNoCinemas.visibility = View.VISIBLE
                rvCinemas.visibility = View.GONE
                Toast.makeText(this, "Lỗi khi tải danh sách rạp phim: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterCinemas(query: String) {
        if (query.isEmpty()) {
            // Nếu không có từ khóa tìm kiếm, hiển thị tất cả rạp phim
            filteredCinemas = allCinemas
        } else {
            // Lọc rạp phim theo từ khóa tìm kiếm
            filteredCinemas = allCinemas.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.address.contains(query, ignoreCase = true) ||
                        it.city.contains(query, ignoreCase = true)
            }
        }

        updateCinemaList()
    }

    private fun updateCinemaList() {
        if (filteredCinemas.isEmpty()) {
            tvNoCinemas.visibility = View.VISIBLE
            rvCinemas.visibility = View.GONE
        } else {
            tvNoCinemas.visibility = View.GONE
            rvCinemas.visibility = View.VISIBLE

            // Hiển thị danh sách rạp phim
            val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                inner class CinemaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
                    val tvName: TextView = itemView.findViewById(R.id.tvCinemaName)
                    val tvAddress: TextView = itemView.findViewById(R.id.tvCinemaAddress)
                    val tvCity: TextView = itemView.findViewById(R.id.tvCinemaCity)
                    val tvPhone: TextView = itemView.findViewById(R.id.tvCinemaPhone)
                    val btnViewMovies: Button = itemView.findViewById(R.id.btnViewMovies)
                }

                override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_cinema, parent, false)
                    return CinemaViewHolder(view)
                }

                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    val cinema = filteredCinemas[position]
                    val viewHolder = holder as CinemaViewHolder

                    viewHolder.tvName.text = cinema.name
                    viewHolder.tvAddress.text = cinema.address
                    viewHolder.tvCity.text = cinema.city
                    viewHolder.tvPhone.text = cinema.phoneNumber

                    // Thiết lập sự kiện click cho item và nút xem phim
                    viewHolder.itemView.setOnClickListener {
                        navigateToCinemaDetail(cinema)
                    }

                    viewHolder.btnViewMovies.setOnClickListener {
                        navigateToCinemaDetail(cinema)
                    }
                }

                override fun getItemCount(): Int = filteredCinemas.size
            }

            rvCinemas.adapter = adapter
        }
    }

    private fun navigateToCinemaDetail(cinema: Cinema) {
        val intent = Intent(this, CinemaDetailActivity::class.java)
        intent.putExtra("CINEMA_ID", cinema.id)
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

