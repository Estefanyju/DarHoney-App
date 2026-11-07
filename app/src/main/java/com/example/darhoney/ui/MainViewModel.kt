package com.example.darhoney.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.darhoney.data.model.Restaurant
import com.example.darhoney.data.model.WishlistItem
import com.example.darhoney.data.repository.AuthRepository
import com.example.darhoney.data.repository.RestaurantRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainViewModel(
    private val authRepo: AuthRepository,
    private val restaurantRepo: RestaurantRepository
) : ViewModel() {

    private val _coupleId = MutableLiveData<String>()
    val coupleId: LiveData<String> = _coupleId

    private val _restaurants = MutableLiveData<List<Restaurant>>(emptyList())
    val restaurants: LiveData<List<Restaurant>> = _restaurants

    private val _wishlist = MutableLiveData<List<WishlistItem>>(emptyList())
    val wishlist: LiveData<List<WishlistItem>> = _wishlist

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun init(coupleId: String) {
        if (_coupleId.value == coupleId) return
        _coupleId.value = coupleId
        viewModelScope.launch {
            restaurantRepo.restaurantsFlow(coupleId).collectLatest {
                _restaurants.value = it
            }
        }
        viewModelScope.launch {
            restaurantRepo.wishlistFlow(coupleId).collectLatest {
                _wishlist.value = it
            }
        }
    }

    fun refresh() {
        _coupleId.value?.let { init(it) }
    }

    fun clearError() { _error.value = null }
}
