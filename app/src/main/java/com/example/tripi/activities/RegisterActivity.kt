package com.example.tripi.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tripi.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)

        binding.registerButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val name = binding.nameEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "The password must contain at least 6 characters.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser


                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()

                        user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {

                                val newUser = hashMapOf(
                                    "name" to name,
                                    "email" to email,
                                    "createdAt" to System.currentTimeMillis()
                                )

                                db.collection("users").document(user.uid)
                                    .set(newUser)
                                    .addOnSuccessListener {

                                        user.sendEmailVerification()
                                            .addOnCompleteListener { verificationTask ->
                                                if (verificationTask.isSuccessful) {
                                                    Toast.makeText(this,
                                                        "A verification email has been sent. Please verify your account.",
                                                        Toast.LENGTH_LONG).show()
                                                }
                                            }

                                        // שמירת מצב התחברות
                                        sharedPref.edit()
                                            .putBoolean("is_logged_in", true)
                                            .putString("user_name", name)
                                            .putString("user_email", email)
                                            .putString("user_id", user.uid)
                                            .apply()


                                        startActivity(Intent(this, MainActivity::class.java))
                                        finish()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this,
                                            "Error saving user data: ${e.localizedMessage}",
                                            Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                    } else {
                        Toast.makeText(this,
                            "Registration failed: ${task.exception?.localizedMessage}",
                            Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}