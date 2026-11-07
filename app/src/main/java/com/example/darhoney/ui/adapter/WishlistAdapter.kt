package com.example.darhoney.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.darhoney.data.model.WishlistItem
import com.example.darhoney.databinding.ItemWishlistBinding

class WishlistAdapter(
    private val onDelete: (WishlistItem) -> Unit,
    private val onConvert: (WishlistItem) -> Unit
) : ListAdapter<WishlistItem, WishlistAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWishlistBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemWishlistBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WishlistItem) {
            binding.tvName.text = item.name
            binding.tvCity.text = item.city
            if (item.address.isNotBlank()) {
                binding.tvAddress.text = item.address
                binding.tvAddress.visibility = android.view.View.VISIBLE
            } else {
                binding.tvAddress.visibility = android.view.View.GONE
            }
            binding.tvReason.text = item.reason
            binding.btnDelete.setOnClickListener { onDelete(item) }
            binding.btnConvert.setOnClickListener { onConvert(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<WishlistItem>() {
        override fun areItemsTheSame(old: WishlistItem, new: WishlistItem) = old.id == new.id
        override fun areContentsTheSame(old: WishlistItem, new: WishlistItem) = old == new
    }
}
