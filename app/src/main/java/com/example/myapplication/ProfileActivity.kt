package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.adapter.BookingAdapter
import com.example.myapplication.model.Booking
import com.example.myapplication.model.User
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var rvBookings: RecyclerView
    private lateinit var tvNoBookings: TextView
    private lateinit var btnAdminPanel: Button
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

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
        tvName = findViewById(R.id.tvName)
        tvEmail = findViewById(R.id.tvEmail)
        rvBookings = findViewById(R.id.rvBookings)
        tvNoBookings = findViewById(R.id.tvNoBookings)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        // Thiết lập bottom navigation
        setupBottomNavigation()

        // Thêm nút Admin Panel nếu là admin
        if (auth.currentUser?.email == "admin@example.com") {
            btnAdminPanel = Button(this)
            btnAdminPanel.text = "Quản lý phim (Admin)"
            btnAdminPanel.setBackgroundColor(resources.getColor(R.color.cgv_red))
            btnAdminPanel.setTextColor(resources.getColor(R.color.white))
            btnAdminPanel.setPadding(16, 16, 16, 16)

            // Thêm vào layout
            val settingsSection = findViewById<LinearLayout>(R.id.settingsSection)
            settingsSection.addView(btnAdminPanel, 0)

            // Thiết lập sự kiện click
            btnAdminPanel.setOnClickListener {
                startActivity(Intent(this, AdminActivity::class.java))
            }
        }

        // Load user profile
        loadUserProfile()

        // Load user bookings
        loadUserBookings()

        // Set up logout button
        findViewById<TextView>(R.id.tvLogout).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Set up other buttons
        findViewById<TextView>(R.id.tvSettings).setOnClickListener {
            Toast.makeText(this, "Cài đặt", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.tvPaymentMethods).setOnClickListener {
            Toast.makeText(this, "Phương thức thanh toán", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.tvNotifications).setOnClickListener {
            Toast.makeText(this, "Thông báo", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.tvHelp).setOnClickListener {
            Toast.makeText(this, "Trợ giúp & Hỗ trợ", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNavigation() {
        // Đánh dấu mục đang chọn
        bottomNavigation.selectedItemId = R.id.nav_profile

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_cinemas -> {
                    startActivity(Intent(this, CinemaListActivity::class.java))
                    true
                }
                R.id.nav_tickets -> {
                    // Đã ở trang profile, không cần chuyển
                    true
                }
                R.id.nav_profile -> {
                    // Đã ở trang profile, không cần chuyển
                    true
                }
                else -> false
            }
        }
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val user = document.toObject(User::class.java)
                        user?.let {
                            tvName.text = it.name
                            tvEmail.text = it.email
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Lỗi khi tải thông tin: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadUserBookings() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("bookings")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { documents ->
                    val bookings = documents.toObjects(Booking::class.java)
                    if (bookings.isEmpty()) {
                        tvNoBookings.visibility = View.VISIBLE
                        rvBookings.visibility = View.GONE
                    } else {
                        tvNoBookings.visibility = View.GONE
                        rvBookings.visibility = View.VISIBLE

                        val adapter = BookingAdapter(bookings) { booking ->
                            val intent = Intent(this, BookingConfirmationActivity::class.java)
                            intent.putExtra("BOOKING_ID", booking.id)
                            startActivity(intent)
                        }
                        rvBookings.adapter = adapter
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Lỗi khi tải đơn đặt vé: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

