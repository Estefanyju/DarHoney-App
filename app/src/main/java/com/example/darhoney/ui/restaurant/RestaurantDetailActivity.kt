package com.example.darhoney.ui.restaurant

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.darhoney.data.model.FavoriteTag
import com.example.darhoney.data.model.FoodType
import com.example.darhoney.data.model.UserRole
import com.example.darhoney.data.repository.RestaurantRepository
import com.example.darhoney.databinding.ActivityRestaurantDetailBinding
import com.example.darhoney.ui.AuthViewModel
import com.example.darhoney.ui.ViewModelFactory
import com.example.darhoney.ui.adapter.VisitAdapter
import com.example.darhoney.utils.FormatUtils
import com.example.darhoney.utils.SessionPreferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class RestaurantDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRestaurantDetailBinding
    private val repository = RestaurantRepository()
    private val authViewModel: AuthViewModel by viewModels { ViewModelFactory.instance }
    private lateinit var visitAdapter: VisitAdapter
    private var coupleId: String = ""
    private var restaurantId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRestaurantDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        coupleId = intent.getStringExtra(EXTRA_COUPLE_ID) ?: ""
        restaurantId = intent.getStringExtra(EXTRA_RESTAURANT_ID) ?: ""

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        visitAdapter = VisitAdapter()
        binding.recyclerVisits.adapter = visitAdapter

        binding.btnAddVisit.setOnClickListener {
            startActivity(Intent(this, AddRestaurantActivity::class.java).apply {
                putExtra(AddRestaurantActivity.EXTRA_COUPLE_ID, coupleId)
                putExtra(AddRestaurantActivity.EXTRA_RESTAURANT_ID, restaurantId)
            })
        }

        binding.btnOpenMaps.setOnClickListener { openInMaps() }

        binding.btnDelete.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Eliminar restaurante")
                .setMessage("¿Estás seguro? Se eliminarán todas las visitas.")
                .setPositiveButton("Eliminar") { _, _ ->
                    lifecycleScope.launch {
                        repository.deleteRestaurant(coupleId, restaurantId)
                        finish()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        updateDeleteVisibility()
        authViewModel.profile.observe(this) { updateDeleteVisibility() }

        loadRestaurant()
    }

    private fun updateDeleteVisibility() {
        val isAdmin = authViewModel.profile.value?.isAdmin() == true ||
            SessionPreferences.getRole(this) == UserRole.ADMIN.name
        binding.btnDelete.visibility = if (isAdmin) View.VISIBLE else View.GONE
    }

    private fun openInMaps() {
        val lat = binding.tvLatitude.text.toString().toDoubleOrNull()
        val lng = binding.tvLongitude.text.toString().toDoubleOrNull()
        val name = binding.tvName.text.toString()
        val address = binding.tvAddress.text.toString()
        val city = binding.tvCity.text.toString()

        val uri = when {
            lat != null && lng != null && lat != 0.0 && lng != 0.0 ->
                Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(name)})")
            address.isNotBlank() || city.isNotBlank() -> {
                val query = listOf(name, address, city).filter { it.isNotBlank() }.joinToString(", ")
                Uri.parse("geo:0,0?q=${Uri.encode(query)}")
            }
            else -> {
                Toast.makeText(this, "Sin ubicación para abrir en Maps", Toast.LENGTH_SHORT).show()
                return
            }
        }
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun loadRestaurant() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val restaurant = repository.getRestaurantWithVisits(coupleId, restaurantId)
                if (restaurant == null) {
                    Toast.makeText(this@RestaurantDetailActivity, "Restaurante no encontrado", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                supportActionBar?.title = restaurant.name
                binding.tvName.text = restaurant.name

                binding.tvFoodType.text = FoodType.fromString(restaurant.foodType).displayName
                binding.tvRating.text = FormatUtils.formatRating(restaurant.averageRating)
                binding.tvLatitude.text = restaurant.latitude.toString()
                binding.tvLongitude.text = restaurant.longitude.toString()

                if (restaurant.address.isNotBlank()) {
                    binding.tvAddress.text = restaurant.address
                    binding.tvAddress.visibility = View.VISIBLE
                } else {
                    binding.tvAddress.visibility = View.GONE
                }

                if (restaurant.city.isNotBlank()) {
                    binding.tvCity.text = restaurant.city
                    binding.tvCity.visibility = View.VISIBLE
                } else {
                    binding.tvCity.visibility = View.GONE
                }

                val tags = restaurant.favoriteTags.mapNotNull { FavoriteTag.fromString(it) }
                if (tags.isNotEmpty()) {
                    binding.tvTags.text = tags.joinToString("  ") { "${it.emoji} ${it.displayName}" }
                    binding.tvTags.visibility = View.VISIBLE
                } else {
                    binding.tvTags.visibility = View.GONE
                }

                visitAdapter.submitList(restaurant.visits)

                val coverPhoto = restaurant.displayCoverPhoto
                if (restaurant.coverPhotoUrl.isBlank() && coverPhoto != null) {
                    repository.updateRestaurant(
                        coupleId,
                        restaurant.copy(coverPhotoUrl = coverPhoto)
                    )
                }

                if (coverPhoto != null) {
                    binding.ivCover.visibility = View.VISIBLE
                    Glide.with(binding.ivCover)
                        .load(coverPhoto)
                        .centerCrop()
                        .into(binding.ivCover)
                } else {
                    binding.ivCover.visibility = View.GONE
                }

                val allPhotos = restaurant.allPhotos
                if (allPhotos.isNotEmpty()) {
                    binding.recyclerPhotos.layoutManager = GridLayoutManager(this@RestaurantDetailActivity, 3)
                    binding.recyclerPhotos.adapter = PhotoGridAdapter(allPhotos)
                }

                restaurant.visits.firstOrNull()?.let { visit ->
                    if (visit.memories.isNotBlank()) {
                        binding.cardMemories.visibility = View.VISIBLE
                        binding.tvMemories.text = visit.memories
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@RestaurantDetailActivity, e.message, Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        const val EXTRA_COUPLE_ID = "couple_id"
        const val EXTRA_RESTAURANT_ID = "restaurant_id"
    }
}

private class PhotoGridAdapter(private val urls: List<String>) :
    androidx.recyclerview.widget.RecyclerView.Adapter<PhotoGridAdapter.VH>() {

    class VH(val imageView: android.widget.ImageView) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
        val iv = android.widget.ImageView(parent.context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 300
            )
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
        }
        return VH(iv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        Glide.with(holder.imageView).load(urls[position]).centerCrop().into(holder.imageView)
    }

    override fun getItemCount() = urls.size
}
