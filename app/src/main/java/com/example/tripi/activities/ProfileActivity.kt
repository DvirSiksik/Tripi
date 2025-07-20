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

    private fun setupBottomNavigation() {
        when (this) {
            is MainActivity -> binding.bottomNavigation.selectedItemId = R.id.navigation_home
            is MyTripsActivity -> binding.bottomNavigation.selectedItemId = R.id.navigation_trips
            is ProfileActivity -> binding.bottomNavigation.selectedItemId = R.id.navigation_profile
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_trips -> {
                    if (this !is MyTripsActivity) {
                        startActivity(Intent(this, MyTripsActivity::class.java))
                        finish()
                    }
                    true
                }
                R.id.navigation_home -> {
                    if (this !is MainActivity) {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                    true
                }
                R.id.navigation_profile -> {
                    if (this !is ProfileActivity) {
                        startActivity(Intent(this, ProfileActivity::class.java))
                        finish()
                    }
                    true
                }
                else -> false
            }
        }
    }
}