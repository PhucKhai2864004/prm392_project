package com.example.myapplication

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.model.Cinema
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.UUID

class CinemaManagementActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private lateinit var rvCinemas: RecyclerView
    private lateinit var tvNoCinemas: TextView
    private lateinit var fabAddCinema: FloatingActionButton

    private val cinemas = mutableListOf<Cinema>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cinema_management)

        // Thiết lập toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Khởi tạo views
        rvCinemas = findViewById(R.id.rvCinemas)
        tvNoCinemas = findViewById(R.id.tvNoCinemas)
        fabAddCinema = findViewById(R.id.fabAddCinema)

        // Thiết lập sự kiện click cho nút thêm rạp phim
        fabAddCinema.setOnClickListener {
            showAddEditCinemaDialog(null)
        }

        // Tải danh sách rạp phim
        loadCinemas()
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
                            val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
                            val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
                        }

                        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                            val view = LayoutInflater.from(parent.context)
                                .inflate(R.layout.item_admin_cinema, parent, false)
                            return CinemaViewHolder(view)
                        }

                        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                            val cinema = cinemas[position]
                            val viewHolder = holder as CinemaViewHolder

                            viewHolder.tvName.text = cinema.name
                            viewHolder.tvAddress.text = cinema.address
                            viewHolder.tvCity.text = cinema.city
                            viewHolder.tvPhone.text = cinema.phoneNumber

                            // Thiết lập sự kiện click cho nút sửa
                            viewHolder.btnEdit.setOnClickListener {
                                showAddEditCinemaDialog(cinema)
                            }

                            // Thiết lập sự kiện click cho nút xóa
                            viewHolder.btnDelete.setOnClickListener {
                                showDeleteCinemaDialog(cinema)
                            }
                        }

                        override fun getItemCount(): Int = cinemas.size
                    }

                    rvCinemas.adapter = adapter
                }
            }
            .addOnFailureListener { e ->
                Log.e("CinemaManagementActivity", "Error loading cinemas: ${e.message}")
                Toast.makeText(this, "Lỗi khi tải danh sách rạp phim: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAddEditCinemaDialog(cinema: Cinema?) {
        val isEdit = cinema != null
        val dialogTitle = if (isEdit) "Chỉnh sửa rạp phim" else "Thêm rạp phim mới"

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_cinema, null)
        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etCinemaName = dialogView.findViewById<EditText>(R.id.etCinemaName)
        val etCinemaAddress = dialogView.findViewById<EditText>(R.id.etCinemaAddress)
        val etCinemaCity = dialogView.findViewById<EditText>(R.id.etCinemaCity)
        val etCinemaPhone = dialogView.findViewById<EditText>(R.id.etCinemaPhone)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        tvDialogTitle.text = dialogTitle

        // Nếu là chỉnh sửa, điền thông tin rạp phim vào form
        if (isEdit) {
            etCinemaName.setText(cinema!!.name)
            etCinemaAddress.setText(cinema.address)
            etCinemaCity.setText(cinema.city)
            etCinemaPhone.setText(cinema.phoneNumber)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val name = etCinemaName.text.toString().trim()
            val address = etCinemaAddress.text.toString().trim()
            val city = etCinemaCity.text.toString().trim()
            val phone = etCinemaPhone.text.toString().trim()

            // Kiểm tra dữ liệu đầu vào
            if (name.isEmpty() || address.isEmpty() || city.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin cơ bản", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Tạo hoặc cập nhật rạp phim
            val cinemaId = cinema?.id ?: UUID.randomUUID().toString()
            val updatedCinema = Cinema(
                id = cinemaId,
                name = name,
                address = address,
                city = city,
                phoneNumber = phone
            )

            saveCinema(updatedCinema, isEdit)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveCinema(cinema: Cinema, isEdit: Boolean) {
        db.collection("cinemas").document(cinema.id)
            .set(cinema)
            .addOnSuccessListener {
                val message = if (isEdit) "Rạp phim đã được cập nhật" else "Rạp phim đã được thêm"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                loadCinemas()
            }
            .addOnFailureListener { e ->
                Log.e("CinemaManagementActivity", "Error saving cinema: ${e.message}")
                Toast.makeText(this, "Lỗi khi lưu rạp phim: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteCinemaDialog(cinema: Cinema) {
        AlertDialog.Builder(this)
            .setTitle("Xóa rạp phim")
            .setMessage("Bạn có chắc chắn muốn xóa rạp phim '${cinema.name}'?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteCinema(cinema)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteCinema(cinema: Cinema) {
        db.collection("cinemas").document(cinema.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Rạp phim đã được xóa", Toast.LENGTH_SHORT).show()
                loadCinemas()
            }
            .addOnFailureListener { e ->
                Log.e("CinemaManagementActivity", "Error deleting cinema: ${e.message}")
                Toast.makeText(this, "Lỗi khi xóa rạp phim: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

