package com.example.myapplication

import android.app.AlertDialog
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.adapter.AdminMovieAdapter
import com.example.myapplication.model.Movie
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.UUID
import java.util.Locale
import java.util.Date

class AdminActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private lateinit var rvMovies: RecyclerView
    private lateinit var tvNoMovies: TextView
    private lateinit var fabAddMovie: FloatingActionButton
    private lateinit var adapter: AdminMovieAdapter
    private var movies = mutableListOf<Movie>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        // Thiết lập toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Khởi tạo views
        rvMovies = findViewById(R.id.rvMovies)
        tvNoMovies = findViewById(R.id.tvNoMovies)
        fabAddMovie = findViewById(R.id.fabAddMovie)

        // Thiết lập adapter
        adapter = AdminMovieAdapter(
            movies,
            onEditClick = { movie ->
                showAddEditMovieDialog(movie)
            },
            onDeleteClick = { movie ->
                showDeleteConfirmationDialog(movie)
            }
        )
        rvMovies.adapter = adapter

        // Tải danh sách phim
        loadMovies()

        // Đăng ký lắng nghe sự kiện cập nhật từ Firestore
        db.collection("movies")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("AdminActivity", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    // Tải lại danh sách phim khi có thay đổi
                    loadMovies()
                }
            }

        // Thiết lập nút thêm phim
        fabAddMovie.setOnClickListener {
            showAddEditMovieDialog(null)
        }
    }

    private fun loadMovies() {
        db.collection("movies")
            .get()
            .addOnSuccessListener { documents ->
                movies.clear()
                for (document in documents) {
                    val movie = document.toObject(Movie::class.java)
                    movies.add(movie)
                }

                if (movies.isEmpty()) {
                    tvNoMovies.visibility = View.VISIBLE
                    rvMovies.visibility = View.GONE
                } else {
                    tvNoMovies.visibility = View.GONE
                    rvMovies.visibility = View.VISIBLE
                    adapter.updateMovies(movies)
                }
            }
            .addOnFailureListener { e ->
                Log.e("AdminActivity", "Error loading movies", e)
                Toast.makeText(this, "Lỗi khi tải danh sách phim: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAddEditMovieDialog(movie: Movie?) {
        val isEdit = movie != null
        val dialogTitle = if (isEdit) "Chỉnh sửa phim" else "Thêm phim mới"

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_movie, null)
        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etMovieTitle = dialogView.findViewById<EditText>(R.id.etMovieTitle)
        val etMovieGenre = dialogView.findViewById<EditText>(R.id.etMovieGenre)
        val etMovieDuration = dialogView.findViewById<EditText>(R.id.etMovieDuration)
        val etMoviePosterUrl = dialogView.findViewById<EditText>(R.id.etMoviePosterUrl)
        val etReleaseDate = dialogView.findViewById<EditText>(R.id.etReleaseDate)
        val etRating = dialogView.findViewById<EditText>(R.id.etRating)
        val etImdbRating = dialogView.findViewById<EditText>(R.id.etImdbRating)
        val etDirector = dialogView.findViewById<EditText>(R.id.etDirector)
        val etCast = dialogView.findViewById<EditText>(R.id.etCast)
        val etSynopsis = dialogView.findViewById<EditText>(R.id.etSynopsis)
        val rgNowShowing = dialogView.findViewById<RadioGroup>(R.id.rgNowShowing)
        val rbYes = dialogView.findViewById<RadioButton>(R.id.rbYes)
        val rbNo = dialogView.findViewById<RadioButton>(R.id.rbNo)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        tvDialogTitle.text = dialogTitle

        // Nếu là chỉnh sửa, điền thông tin phim vào form
        if (isEdit) {
            etMovieTitle.setText(movie!!.title)
            etMovieGenre.setText(movie.genre)
            etMovieDuration.setText(movie.duration.replace(" phút", ""))
            etMoviePosterUrl.setText(movie.posterUrl)

            // Điền các trường mới
            if (movie.releaseDate != null) {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                etReleaseDate.setText(dateFormat.format(movie.releaseDate))
            }
            etRating.setText(movie.rating)
            etImdbRating.setText(movie.imdbRating)
            etDirector.setText(movie.director)
            etCast.setText(movie.cast.joinToString(", "))
            etSynopsis.setText(movie.synopsis)

            if (movie.nowShowing) rbYes.isChecked = true else rbNo.isChecked = true
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val title = etMovieTitle.text.toString().trim()
            val genre = etMovieGenre.text.toString().trim()
            val durationStr = etMovieDuration.text.toString().trim()
            val posterUrl = etMoviePosterUrl.text.toString().trim()
            val releaseDateStr = etReleaseDate.text.toString().trim()
            val rating = etRating.text.toString().trim()
            val imdbRating = etImdbRating.text.toString().trim()
            val director = etDirector.text.toString().trim()
            val castStr = etCast.text.toString().trim()
            val synopsis = etSynopsis.text.toString().trim()
            val isNowShowing = rgNowShowing.checkedRadioButtonId == R.id.rbYes

            // Kiểm tra dữ liệu đầu vào
            if (title.isEmpty() || genre.isEmpty() || durationStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin cơ bản", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val duration = "$durationStr phút"

            // Xử lý ngày khởi chiếu
            var releaseDate: Date? = null
            if (releaseDateStr.isNotEmpty()) {
                try {
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    releaseDate = dateFormat.parse(releaseDateStr)
                } catch (e: Exception) {
                    Toast.makeText(this, "Định dạng ngày không hợp lệ. Sử dụng dd/MM/yyyy", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Xử lý danh sách diễn viên
            val castList = if (castStr.isEmpty()) listOf() else castStr.split(",").map { it.trim() }

            // Tạo hoặc cập nhật phim
            val movieId = movie?.id ?: UUID.randomUUID().toString()
            val updatedMovie = Movie(
                id = movieId,
                title = title,
                genre = genre,
                duration = duration,
                posterUrl = posterUrl,
                nowShowing = isNowShowing,
                releaseDate = releaseDate,
                rating = rating,
                imdbRating = imdbRating,
                director = director,
                cast = castList,
                synopsis = synopsis
            )

            saveMovie(updatedMovie, isEdit)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveMovie(movie: Movie, isEdit: Boolean) {
        db.collection("movies").document(movie.id)
            .set(movie)
            .addOnSuccessListener {
                val message = if (isEdit) "Cập nhật phim thành công" else "Thêm phim mới thành công"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

                // Xóa cache của Glide
                Thread {
                    Glide.get(applicationContext).clearDiskCache()
                }.start()
                Glide.get(applicationContext).clearMemory()

                // Tải lại danh sách phim
                loadMovies()
            }
            .addOnFailureListener { e ->
                val action = if (isEdit) "cập nhật" else "thêm"
                Toast.makeText(this, "Lỗi khi $action phim: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteConfirmationDialog(movie: Movie) {
        AlertDialog.Builder(this)
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc chắn muốn xóa phim '${movie.title}'?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteMovie(movie)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteMovie(movie: Movie) {
        db.collection("movies").document(movie.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Xóa phim thành công", Toast.LENGTH_SHORT).show()
                loadMovies() // Tải lại danh sách phim
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi khi xóa phim: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}