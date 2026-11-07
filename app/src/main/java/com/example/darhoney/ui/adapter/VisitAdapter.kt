package com.example.darhoney.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.darhoney.R
import com.example.darhoney.data.model.Visit
import com.example.darhoney.databinding.ItemVisitBinding
import com.example.darhoney.utils.FormatUtils

class VisitAdapter : ListAdapter<Visit, VisitAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVisitBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemVisitBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(visit: Visit) {
            binding.tvDate.text = FormatUtils.formatDate(visit.date)
            binding.tvRating.text = FormatUtils.formatRating(visit.ratings.average())
            binding.tvComments.text = visit.comments.ifBlank { visit.memories }
            binding.tvPhotoCount.text = binding.root.context.getString(
                R.string.photo_count, visit.photoUrls.size
            )

            val photo = visit.photoUrls.firstOrNull()
            if (photo != null) {
                Glide.with(binding.ivPhoto).load(photo)
                    .centerCrop()
                    .placeholder(R.drawable.ic_restaurant_placeholder)
                    .error(R.drawable.ic_restaurant_placeholder)
                    .into(binding.ivPhoto)
            } else {
                binding.ivPhoto.setImageResource(R.drawable.ic_restaurant_placeholder)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Visit>() {
        override fun areItemsTheSame(old: Visit, new: Visit) = old.id == new.id
        override fun areContentsTheSame(old: Visit, new: Visit) = old == new
    }
}
