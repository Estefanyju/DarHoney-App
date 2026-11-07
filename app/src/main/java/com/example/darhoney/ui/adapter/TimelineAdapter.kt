package com.example.darhoney.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.darhoney.R
import com.example.darhoney.data.model.Restaurant
import com.example.darhoney.databinding.ItemTimelineBinding
import com.example.darhoney.utils.FormatUtils

class TimelineAdapter : ListAdapter<TimelineAdapter.TimelineEntry, TimelineAdapter.ViewHolder>(DiffCallback()) {

    data class TimelineEntry(
        val restaurant: Restaurant,
        val visitDate: Long,
        val rating: Float,
        val comments: String,
        val photoUrl: String?
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTimelineBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemTimelineBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: TimelineEntry) {
            binding.tvRestaurant.text = entry.restaurant.name
            binding.tvCity.text = entry.restaurant.city
            binding.tvDate.text = FormatUtils.formatDate(entry.visitDate)
            binding.tvRating.text = FormatUtils.formatRating(entry.rating)
            binding.tvComments.text = entry.comments

            if (entry.photoUrl != null) {
                Glide.with(binding.ivPhoto).load(entry.photoUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_restaurant_placeholder)
                    .error(R.drawable.ic_restaurant_placeholder)
                    .into(binding.ivPhoto)
            } else {
                binding.ivPhoto.setImageResource(R.drawable.ic_restaurant_placeholder)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TimelineEntry>() {
        override fun areItemsTheSame(old: TimelineEntry, new: TimelineEntry) =
            old.restaurant.id == new.restaurant.id && old.visitDate == new.visitDate
        override fun areContentsTheSame(old: TimelineEntry, new: TimelineEntry) = old == new
    }
}
