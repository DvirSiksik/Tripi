package com.example.tripi.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.tripi.R
import com.example.tripi.databinding.ActivityProfileBinding
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

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
                loadTripsData(user.uid)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadTripsData(userId: String) {
        val currentUser = auth.currentUser
        val userEmail = currentUser?.email ?: ""
        val myTripsQuery = db.collection("trips").whereEqualTo("creatorId", userId)
        val sharedTripsQuery = db.collection("trips")
            .whereArrayContainsAny("sharedWith", listOf(userId, userEmail))

        Tasks.whenAllSuccess<QuerySnapshot>(
            myTripsQuery.get(),
            sharedTripsQuery.get()
        ).addOnSuccessListener { results ->
            val myTrips = (results[0] as QuerySnapshot)
            val sharedTrips = (results[1] as QuerySnapshot)
            val myTripIds = myTrips.documents.map { it.id }
            val uniqueSharedTrips = sharedTrips.documents.filterNot { myTripIds.contains(it.id) }
            val myTripsCount = myTrips.size()
            val sharedTripsCount = uniqueSharedTrips.size
            val totalTrips = myTripsCount + sharedTripsCount
            binding.totalTripsTextView.text = "$totalTrips Trips"
            binding.myTripsCountTextView.text = "$myTripsCount My Trips"
            binding.sharedTripsCountTextView.text = "$sharedTripsCount Shared With Me"
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load trips data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupListeners() {
        binding.logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }

        binding.editProfileButton.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
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
            else -> R.id.navigation_home
        }
    }
    private fun setupBottomNavigation() {
        updateBottomNavSelection()

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val targetActivity = when (item.itemId) {
                R.id.navigation_home -> MainActivity::class.java
                R.id.navigation_trips -> MyTripsActivity::class.java
                R.id.navigation_profile -> ProfileActivity::class.java
                else -> return@setOnItemSelectedListener false
            }

            if (this::class.java == targetActivity) {
                return@setOnItemSelectedListener true
            }

            startActivity(Intent(this, targetActivity).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            true
        }
    }
}