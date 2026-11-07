package com.example.darhoney.ui.search

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.darhoney.R
import com.example.darhoney.data.model.FavoriteTag
import com.example.darhoney.data.model.FoodType
import com.example.darhoney.data.model.PriceRange
import com.example.darhoney.data.repository.RestaurantRepository
import com.example.darhoney.databinding.ActivitySearchBinding
import com.example.darhoney.ui.adapter.RestaurantAdapter
import com.example.darhoney.ui.restaurant.RestaurantDetailActivity
import com.example.darhoney.utils.SearchFilter
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private val repository = RestaurantRepository()
    private lateinit var adapter: RestaurantAdapter
    private var coupleId: String = ""
    private var allRestaurants = listOf<com.example.darhoney.data.model.Restaurant>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        coupleId = intent.getStringExtra(EXTRA_COUPLE_ID) ?: ""

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Búsqueda avanzada"
        }

        adapter = RestaurantAdapter { restaurant ->
            startActivity(Intent(this, RestaurantDetailActivity::class.java).apply {
                putExtra(RestaurantDetailActivity.EXTRA_COUPLE_ID, coupleId)
                putExtra(RestaurantDetailActivity.EXTRA_RESTAURANT_ID, restaurant.id)
            })
        }
        binding.recyclerView.adapter = adapter

        setupFilters()
        loadRestaurants()
    }

    private fun setupFilters() {
        setupDropdown(
            binding.actvFoodType,
            listOf("Todos") + FoodType.entries.map { it.displayName }
        )
        setupDropdown(
            binding.actvPrice,
            listOf("Todos") + PriceRange.entries.map { it.displayName }
        )
        setupDropdown(
            binding.actvTag,
            listOf("Todos") + FavoriteTag.entries.map { "${it.emoji} ${it.displayName}" }
        )
        setupDropdown(
            binding.actvRating,
            listOf("Cualquiera", "3+ estrellas", "4+ estrellas", "5 estrellas")
        )

        binding.btnSearch.setOnClickListener { applyFilters() }
        binding.etQuery.setOnEditorActionListener { _, _, _ ->
            applyFilters()
            true
        }
    }

    private fun setupDropdown(view: AutoCompleteTextView, items: List<String>) {
        val adapter = ArrayAdapter(this, R.layout.item_spinner, items)
        view.setAdapter(adapter)
        if (items.isNotEmpty()) {
            view.setText(items.first(), false)
        }
    }

    private fun loadRestaurants() {
        lifecycleScope.launch {
            repository.restaurantsFlow(coupleId).collect { restaurants ->
                allRestaurants = restaurants
                adapter.submitList(restaurants)
            }
        }
    }

    private fun applyFilters() {
        val query = binding.etQuery.text.toString()

        val foodTypeSelection = binding.actvFoodType.text.toString()
        val foodType = if (foodTypeSelection == "Todos") null
        else FoodType.entries.find { it.displayName == foodTypeSelection }

        val priceSelection = binding.actvPrice.text.toString()
        val priceRange = if (priceSelection == "Todos") null
        else PriceRange.entries.find { it.displayName == priceSelection }

        val tagSelection = binding.actvTag.text.toString()
        val tag = if (tagSelection == "Todos") null
        else FavoriteTag.entries.find { "${it.emoji} ${it.displayName}" == tagSelection }

        val minRating = when (binding.actvRating.text.toString()) {
            "3+ estrellas" -> 3f
            "4+ estrellas" -> 4f
            "5 estrellas" -> 5f
            else -> 0f
        }

        val city = binding.etCity.text.toString()

        val filtered = SearchFilter.filter(
            allRestaurants, query, foodType, minRating, priceRange, tag, city
        )
        adapter.submitList(filtered)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        const val EXTRA_COUPLE_ID = "couple_id"
    }
}
