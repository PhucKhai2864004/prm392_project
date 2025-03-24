package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityRegisterBinding
import com.example.myapplication.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Set up click listeners
        binding.btnRegister.setOnClickListener {
            registerUser()
        }

        binding.tvLoginPrompt.setOnClickListener {
            finish()
        }
    }

    private fun registerUser() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        // Validate input
        if (name.isEmpty()) {
            binding.tilName.error = "Họ tên không được để trống"
            return
        }

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email không được để trống"
            return
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Mật khẩu không được để trống"
            return
        }

        if (password.length < 6) {
            binding.tilPassword.error = "Mật khẩu phải có ít nhất 6 ký tự"
            return
        }

        if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Mật khẩu xác nhận không khớp"
            return
        }

        // Show progress bar
        binding.progressBar.visibility = View.VISIBLE

        // Create user with email and password
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign up success, save user data to Firestore
                    val firebaseUser = auth.currentUser
                    firebaseUser?.let {
                        val user = User(
                            uid = it.uid,
                            name = name,
                            email = email
                        )

                        // Save user to Firestore
                        db.collection("users").document(it.uid)
                            .set(user)
                            .addOnSuccessListener {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(this, "Đăng ký thành công", Toast.LENGTH_SHORT).show()
                                navigateToMainActivity()
                            }
                            .addOnFailureListener { e ->
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(this, "Lỗi khi lưu thông tin người dùng: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    // If sign up fails, display a message to the user
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        baseContext, "Đăng ký thất bại: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

