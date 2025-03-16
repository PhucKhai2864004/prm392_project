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
import com.example.myapplication.model.User
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
            Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.tvPaymentMethods).setOnClickListener {
            Toast.makeText(this, "Payment Methods clicked", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.tvNotifications).setOnClickListener {
            Toast.makeText(this, "Notifications clicked", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.tvHelp).setOnClickListener {
            Toast.makeText(this, "Help & Support clicked", Toast.LENGTH_SHORT).show()
        }

        // Kiểm tra nếu người dùng là admin (email admin@example.com)
//        if (auth.currentUser?.email == "admin@example.com") {
//            // Thêm nút Admin vào layout
//            val adminButton = TextView(this)
//            adminButton.id = View.generateViewId()
//            adminButton.text = "Quản lý phim (Admin)"
//            adminButton.setTextColor(resources.getColor(R.color.cgv_red))
//            adminButton.setPadding(16, 16, 16, 16)
//            adminButton.setBackgroundResource(android.R.attr.selectableItemBackground)
//
//            // Thêm vào layout
//            val cardView = findViewById<View>(R.id.cardSettings)
//            if (cardView is androidx.cardview.widget.CardView) {
//                val linearLayout = cardView.getChildAt(0) as? android.widget.LinearLayout
//                linearLayout?.addView(adminButton, 0)
//
//                // Thêm đường kẻ dưới nút Admin
//                val divider = View(this)
//                divider.layoutParams = android.widget.LinearLayout.LayoutParams(
//                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
//                    1
//                )
//                divider.setBackgroundColor(resources.getColor(R.color.cgv_light_gray))
//                linearLayout?.addView(divider, 1)
//
//                // Thiết lập sự kiện click
//                adminButton.setOnClickListener {
//                    startActivity(Intent(this, AdminActivity::class.java))
//                }
//            }
//        }
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
                    Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this, "Error loading bookings: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}