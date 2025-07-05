package com.example.tripi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tripi.R
import com.example.tripi.models.ItineraryItem
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class ItineraryAdapter(
    private val items: List<ItineraryItem>,
    private val onItemClick: (ItineraryItem) -> Unit
) : RecyclerView.Adapter<ItineraryAdapter.ItineraryViewHolder>() {

    inner class ItineraryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.itemTitleTextView)
        private val time: TextView = itemView.findViewById(R.id.itemTimeTextView)
        private val category: TextView = itemView.findViewById(R.id.itemCategoryTextView)

        fun bind(item: ItineraryItem) {
            title.text = item.title

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            time.text = timeFormat.format(item.time.toDate())

            category.text = item.category

            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItineraryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_itinerary, parent, false)
        return ItineraryViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItineraryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}