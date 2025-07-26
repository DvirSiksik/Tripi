package com.example.tripi.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
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
import com.example.tripi.databinding.ActivityEditProfileBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var selectedImageUri: Uri? = null
    private var currentUser: FirebaseUser? = null

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
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    selectedImageUri = uri
                    Glide.with(this)
                        .load(uri)
                        .circleCrop()
                        .into(binding.profileImageView)
                }
            }
        } catch (e: Exception) {
            Log.e("EditProfile", "Error selecting image", e)
            Toast.makeText(this, getString(R.string.error_selecting_image), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.user_not_authenticated), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadUserData()
        setupListeners()
    }

    private fun loadUserData() {
        currentUser?.let { user ->
            db.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        binding.nameEditText.setText(document.getString("name") ?: "")
                        binding.emailEditText.setText(user.email ?: "")

                        document.getString("profileImage")?.takeIf { it.isNotEmpty() }?.let { url ->
                            Glide.with(this)
                                .load(url)
                                .circleCrop()
                                .into(binding.profileImageView)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("EditProfile", "Error loading user data", e)
                    Toast.makeText(this, getString(R.string.error_loading_profile), Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setupListeners() {
        binding.profileImageView.setOnClickListener {
            if (hasImagePermission()) {
                openImagePicker()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        binding.saveButton.setOnClickListener {
            updateProfile()
        }

        binding.changePasswordButton.setOnClickListener {
            changePassword()
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
            Log.e("EditProfile", "Error opening image picker", e)
            Toast.makeText(this, getString(R.string.error_opening_picker), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateProfile() {
        val name = binding.nameEditText.text.toString().trim()
        val newEmail = binding.emailEditText.text.toString().trim()

        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.user_not_authenticated), Toast.LENGTH_SHORT).show()
            return
        }

        if (name.isEmpty()) {
            binding.nameEditText.error = getString(R.string.name_required)
            return
        }

        if (newEmail.isEmpty()) {
            binding.emailEditText.error = getString(R.string.email_required)
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        if (selectedImageUri != null) {
            uploadImageAndUpdateProfile(name, newEmail)
        } else {
            currentUser?.let { user ->
                updateUserData(name, newEmail, user.photoUrl?.toString())
            }
        }
    }

    private fun uploadImageAndUpdateProfile(name: String, newEmail: String) {
        currentUser?.let { user ->
            val storageRef = storage.reference
            val imageRef = storageRef.child("profile_images/${user.uid}/${UUID.randomUUID()}")

            selectedImageUri?.let { uri ->
                imageRef.putFile(uri)
                    .addOnSuccessListener { taskSnapshot ->
                        imageRef.downloadUrl
                            .addOnSuccessListener { downloadUri ->
                                updateUserData(name, newEmail, downloadUri.toString())
                            }
                            .addOnFailureListener { e ->
                                binding.progressBar.visibility = View.GONE
                                Log.e("EditProfile", "Error getting download URL", e)
                                Toast.makeText(
                                    this,
                                    getString(R.string.error_getting_url),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        binding.progressBar.visibility = View.GONE
                        Log.e("EditProfile", "Error uploading image", e)
                        Toast.makeText(
                            this,
                            getString(R.string.error_uploading_image),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            } ?: run {
                updateUserData(name, newEmail, null)
            }
        }
    }

    private fun updateUserData(name: String, newEmail: String, profileImageUrl: String?) {
        currentUser?.let { user ->
            if (newEmail != user.email) {
                user.updateEmail(newEmail)
                    .addOnCompleteListener { emailTask ->
                        if (emailTask.isSuccessful) {
                            updateFirestoreData(user.uid, name, newEmail, profileImageUrl)
                        } else {
                            binding.progressBar.visibility = View.GONE
                            Log.e("EditProfile", "Email update failed", emailTask.exception)
                            Toast.makeText(
                                this,
                                getString(R.string.error_updating_email, emailTask.exception?.message),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            } else {
                updateFirestoreData(user.uid, name, newEmail, profileImageUrl)
            }
        }
    }

    private fun updateFirestoreData(userId: String, name: String, email: String, profileImageUrl: String?) {
        val updates = hashMapOf<String, Any>(
            "name" to name,
            "email" to email,
            "updatedAt" to System.currentTimeMillis()
        )

        profileImageUrl?.let { url ->
            updates["profileImage"] = url
        }

        db.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    this,
                    getString(R.string.profile_updated_successfully),
                    Toast.LENGTH_SHORT
                ).show()
                setResult(Activity.RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Log.e("EditProfile", "Error updating Firestore", e)
                Toast.makeText(
                    this,
                    getString(R.string.error_updating_profile, e.localizedMessage),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun changePassword() {
        val currentPassword = binding.currentPasswordEditText.text.toString()
        val newPassword = binding.newPasswordEditText.text.toString()
        val confirmPassword = binding.confirmPasswordEditText.text.toString()

        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.user_not_authenticated), Toast.LENGTH_SHORT).show()
            return
        }

        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(this, getString(R.string.passwords_dont_match), Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword.length < 6) {
            Toast.makeText(this, getString(R.string.password_too_short), Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        currentUser?.email?.let { email ->
            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            currentUser?.reauthenticate(credential)
                ?.addOnSuccessListener {
                    currentUser?.updatePassword(newPassword)
                        ?.addOnSuccessListener {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(
                                this,
                                getString(R.string.password_updated_successfully),
                                Toast.LENGTH_SHORT
                            ).show()
                            binding.currentPasswordEditText.text?.clear()
                            binding.newPasswordEditText.text?.clear()
                            binding.confirmPasswordEditText.text?.clear()
                        }
                        ?.addOnFailureListener { e ->
                            binding.progressBar.visibility = View.GONE
                            Log.e("EditProfile", "Password update failed", e)
                            Toast.makeText(
                                this,
                                getString(R.string.error_updating_password, e.message),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                ?.addOnFailureListener { e ->
                    binding.progressBar.visibility = View.GONE
                    Log.e("EditProfile", "Reauthentication failed", e)
                    Toast.makeText(
                        this,
                        getString(R.string.incorrect_current_password),
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }
}