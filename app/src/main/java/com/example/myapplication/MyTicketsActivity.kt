package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.adapter.BookingAdapter
import com.example.myapplication.model.Booking
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MyTicketsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    private lateinit var rvBookings: RecyclerView
    private lateinit var tvNoBookings: TextView
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_tickets)

        // Thiết lập toolbar
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

        // Khởi tạo views
        rvBookings = findViewById(R.id.rvBookings)
        tvNoBookings = findViewById(R.id.tvNoBookings)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        // Thiết lập bottom navigation
        setupBottomNavigation()

        // Tải danh sách vé
        loadUserBookings()
    }

    private fun setupBottomNavigation() {
        // Đánh dấu mục đang chọn
        bottomNavigation.selectedItemId = R.id.nav_tickets

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
                    // Đã ở trang vé của tôi, không cần chuyển
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
                            val intent = Intent(this, TicketDetailActivity::class.java)
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

