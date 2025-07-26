package com.example.tripi.activities

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.tripi.R
import com.example.tripi.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var sharedPref: SharedPreferences
    private var selectedImageUri: Uri? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    selectedImageUri = uri
                    Glide.with(this)
                        .load(uri)
                        .circleCrop()
                        .into(binding.profileImageView)
                }
            }
        } catch (e: Exception) {
            Log.e("RegisterActivity", "Error selecting image", e)
            Toast.makeText(this, getString(R.string.error_selecting_image), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)

        binding.profileImageView.setOnClickListener {
            if (hasImagePermission()) {
                openImagePicker()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        binding.registerButton.setOnClickListener {
            registerUser()
        }
    }

    private fun hasImagePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            true // No permission needed for Android 13+
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun openImagePicker() {
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            pickImage.launch(intent)
        } catch (e: Exception) {
            Log.e("RegisterActivity", "Error opening image picker", e)
            Toast.makeText(this, getString(R.string.error_opening_picker), Toast.LENGTH_SHORT).show()
        }
    }

    private fun registerUser() {
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()
        val name = binding.nameEditText.text.toString().trim()
        val phone = binding.phoneEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, getString(R.string.password_too_short), Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (selectedImageUri != null) {
                        uploadImageAndRegister(user, name, email, phone)
                    } else {
                        completeRegistration(user, name, email, phone, null)
                    }
                } else {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this,
                        getString(R.string.registration_failed, task.exception?.localizedMessage),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun uploadImageAndRegister(user: FirebaseUser?, name: String, email: String, phone: String) {
        user?.let {
            val storageRef = storage.reference
            val imageRef = storageRef.child("profile_images/${user.uid}/${UUID.randomUUID()}")

            selectedImageUri?.let { uri ->
                imageRef.putFile(uri)
                    .addOnSuccessListener { taskSnapshot ->
                        imageRef.downloadUrl
                            .addOnSuccessListener { downloadUri ->
                                completeRegistration(user, name, email, phone, downloadUri.toString())
                            }
                            .addOnFailureListener { e ->
                                binding.progressBar.visibility = View.GONE
                                Log.e("RegisterActivity", "Error getting download URL", e)
                                Toast.makeText(
                                    this,
                                    getString(R.string.error_getting_url),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        binding.progressBar.visibility = View.GONE
                        Log.e("RegisterActivity", "Error uploading image", e)
                        Toast.makeText(
                            this,
                            getString(R.string.error_uploading_image),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }
    }

    private fun completeRegistration(user: FirebaseUser?, name: String, email: String, phone: String, profileImageUrl: String?) {
        user?.let {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .setPhotoUri(profileImageUrl?.let { Uri.parse(it) })
                .build()

            user.updateProfile(profileUpdates).addOnCompleteListener { profileTask ->
                if (profileTask.isSuccessful) {
                    val userData = hashMapOf(
                        "name" to name,
                        "email" to email,
                        "phone" to phone,
                        "profileImage" to (profileImageUrl ?: ""),
                        "createdAt" to System.currentTimeMillis()
                    )

                    db.collection("users").document(user.uid)
                        .set(userData)
                        .addOnSuccessListener {
                            user.sendEmailVerification()
                                .addOnCompleteListener { verificationTask ->
                                    binding.progressBar.visibility = View.GONE
                                    if (verificationTask.isSuccessful) {
                                        saveToSharedPreferences(name, email, phone, user.uid, profileImageUrl)
                                        Toast.makeText(
                                            this,
                                            getString(R.string.registration_success_with_verification),
                                            Toast.LENGTH_LONG
                                        ).show()
                                        startActivity(Intent(this, MainActivity::class.java))
                                        finish()
                                    } else {
                                        Toast.makeText(
                                            this,
                                            getString(R.string.error_sending_verification),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                        }
                        .addOnFailureListener { e ->
                            binding.progressBar.visibility = View.GONE
                            Log.e("RegisterActivity", "Error saving user data", e)
                            Toast.makeText(
                                this,
                                getString(R.string.error_saving_user_data, e.localizedMessage),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    binding.progressBar.visibility = View.GONE
                    Log.e("RegisterActivity", "Error updating profile", profileTask.exception)
                    Toast.makeText(
                        this,
                        getString(R.string.error_updating_profile),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun saveToSharedPreferences(name: String, email: String, phone: String, userId: String, profileImageUrl: String?) {
        sharedPref.edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_name", name)
            putString("user_email", email)
            putString("user_phone", phone)
            putString("user_id", userId)
            putString("profile_image", profileImageUrl ?: "")
            apply()
        }
    }
}