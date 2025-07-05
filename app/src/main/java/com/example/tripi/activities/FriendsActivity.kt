package com.example.tripi.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tripi.R
import com.example.tripi.adapters.FriendsAdapter
import com.example.tripi.databinding.ActivityFriendsBinding
import com.example.tripi.models.User

class FriendsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFriendsBinding
    private lateinit var friendsAdapter: FriendsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFriendsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupToolbar()
    }

    private fun setupRecyclerView() {
        friendsAdapter = FriendsAdapter(getSampleFriends()) { _ -> }
        binding.friendsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@FriendsActivity)
            adapter = friendsAdapter
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Friends"
    }

    private fun getSampleFriends(): List<User> {
        return listOf(
            User(
                name = "John Doe",
                email = "john@example.com",
                imageRes = R.drawable.ic_trip_placeholder
            ),
            User(
                name = "Jane Smith",
                email = "jane@example.com",
                imageRes = R.drawable.ic_trip_placeholder
            )
        )
    }
}