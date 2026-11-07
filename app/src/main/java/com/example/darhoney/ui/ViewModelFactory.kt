package com.example.darhoney.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.darhoney.data.repository.AuthRepository
import com.example.darhoney.data.repository.CoupleRepository
import com.example.darhoney.data.repository.RestaurantRepository

class ViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) ->
                AuthViewModel(AuthRepository(), CoupleRepository()) as T
            modelClass.isAssignableFrom(MainViewModel::class.java) ->
                MainViewModel(AuthRepository(), RestaurantRepository()) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }

    companion object {
        val instance = ViewModelFactory()
    }
}
