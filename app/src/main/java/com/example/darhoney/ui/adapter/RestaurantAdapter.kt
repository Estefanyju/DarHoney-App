package com.example.darhoney.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.darhoney.R
import com.example.darhoney.data.model.FavoriteTag
import com.example.darhoney.data.model.FoodType
import com.example.darhoney.data.model.Restaurant
import com.example.darhoney.databinding.ItemRestaurantBinding
import com.example.darhoney.utils.FormatUtils

class RestaurantAdapter(
    private val onClick: (Restaurant) -> Unit
) : ListAdapter<Restaurant, RestaurantAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRestaurantBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemRestaurantBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(restaurant: Restaurant) {
            binding.tvName.text = restaurant.name
            binding.tvCity.text = restaurant.city
            binding.tvFoodType.text = FoodType.fromString(restaurant.foodType).displayName
            binding.tvRating.text = FormatUtils.formatRating(restaurant.averageRating)
            binding.tvVisits.text = binding.root.context.getString(
                R.string.visit_count, restaurant.visitCount
            )

            val tags = restaurant.favoriteTags.mapNotNull { FavoriteTag.fromString(it) }
            binding.tvTags.text = tags.joinToString(" ") { it.emoji }

            val photo = restaurant.displayCoverPhoto
            if (photo != null) {
                Glide.with(binding.ivPhoto).load(photo)
                    .centerCrop()
                    .placeholder(R.drawable.ic_restaurant_placeholder)
                    .error(R.drawable.ic_restaurant_placeholder)
                    .into(binding.ivPhoto)
            } else {
                binding.ivPhoto.setImageResource(R.drawable.ic_restaurant_placeholder)
            }

            binding.root.setOnClickListener { onClick(restaurant) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Restaurant>() {
        override fun areItemsTheSame(old: Restaurant, new: Restaurant) = old.id == new.id
        override fun areContentsTheSame(old: Restaurant, new: Restaurant) = old == new
    }
}
