package com.example.tripi.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.tripi.R
import com.example.tripi.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupBottomNavigation()
        loadUserData()
        setupListeners()
    }

    private fun loadUserData() {
        val user = auth.currentUser ?: return

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val name = document.getString("name") ?: ""
                val email = document.getString("email") ?: ""
                val profileImage = document.getString("profileImage") ?: ""

                binding.userNameTextView.text = name
                binding.userEmailTextView.text = email

                if (profileImage.isNotEmpty()) {
                    Glide.with(this)
                        .load(profileImage)
                        .circleCrop()
                        .into(binding.profileImageView)
                }
            }
    }

    private fun setupListeners() {
        binding.logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }

        binding.editProfileButton.setOnClickListener {
            // Implement profile editing
        }
    }

    override fun onResume() {
        super.onResume()
        updateBottomNavSelection()
    }

    private fun updateBottomNavSelection() {
        binding.bottomNavigation.selectedItemId = when (this) {
            is MainActivity -> R.id.navigation_home
            is MyTripsActivity -> R.id.navigation_trips
            is ProfileActivity -> R.id.navigation_profile
            else -> R.id.navigation_home // default
        }
    }

    private fun setupBottomNavigation() {
        updateBottomNavSelection() // עדכן בהתחלה

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val targetActivity = when (item.itemId) {
                R.id.navigation_home -> MainActivity::class.java
                R.id.navigation_trips -> MyTripsActivity::class.java
                R.id.navigation_profile -> ProfileActivity::class.java
                else -> return@setOnItemSelectedListener false
            }

            if (this::class.java == targetActivity) {
                return@setOnItemSelectedListener true // כבר בטאב הזה
            }

            startActivity(Intent(this, targetActivity).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            true
        }
    }
}