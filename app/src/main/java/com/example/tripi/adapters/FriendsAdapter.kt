package com.example.tripi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tripi.R
import com.example.tripi.models.User

class FriendsAdapter(
    private var friends: List<User>,
    private val onItemClick: (User) -> Unit
) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {


    inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.friendName)
        val email: TextView = itemView.findViewById(R.id.friendEmail)
        val image: ImageView = itemView.findViewById(R.id.friendImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friends[position]
        with(holder) {
            name.text = friend.name
            email.text = friend.email

            if (friend.imageUrl != null) {
                Glide.with(itemView.context)
                    .load(friend.imageUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(image)
            } else if (friend.imageRes != null) {
                image.setImageResource(friend.imageRes)
            } else {
                image.setImageResource(R.drawable.ic_profile_placeholder)
            }

            itemView.setOnClickListener { onItemClick(friend) }
        }
    }

    override fun getItemCount(): Int = friends.size


    fun updateData(newFriends: List<User>) {
        friends = newFriends.toList()  // Create a new list instead of casting
        notifyDataSetChanged()
    }
}