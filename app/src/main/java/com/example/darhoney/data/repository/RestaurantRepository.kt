package com.example.darhoney.data.repository

import com.example.darhoney.data.model.Restaurant
import com.example.darhoney.data.model.Visit
import com.example.darhoney.data.model.WishlistItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class RestaurantRepository {
    private val db = FirebaseFirestore.getInstance()

    private fun restaurantsRef(coupleId: String) =
        db.collection("couples").document(coupleId).collection("restaurants")

    private fun visitsRef(coupleId: String, restaurantId: String) =
        restaurantsRef(coupleId).document(restaurantId).collection("visits")

    private fun wishlistRef(coupleId: String) =
        db.collection("couples").document(coupleId).collection("wishlist")

    fun restaurantsFlow(coupleId: String): Flow<List<Restaurant>> = callbackFlow {
        if (coupleId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val restaurantListener = restaurantsRef(coupleId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val restaurants = snapshot?.documents?.map { doc ->
                    Restaurant.fromMap(doc.id, doc.data ?: emptyMap())
                } ?: emptyList()

                if (restaurants.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                var loaded = 0
                val result = mutableListOf<Restaurant>()
                restaurants.forEach { restaurant ->
                    visitsRef(coupleId, restaurant.id)
                        .orderBy("date", Query.Direction.DESCENDING)
                        .get()
                        .addOnSuccessListener { visitsSnapshot ->
                            val visits = visitsSnapshot.documents.map { vDoc ->
                                Visit.fromMap(vDoc.id, vDoc.data ?: emptyMap())
                            }
                            result.add(restaurant.copy(visits = visits))
                            loaded++
                            if (loaded == restaurants.size) {
                                trySend(result.sortedByDescending { it.latestVisit?.date ?: it.createdAt })
                            }
                        }
                }
            }
        awaitClose { restaurantListener.remove() }
    }

    suspend fun addRestaurant(coupleId: String, restaurant: Restaurant, visit: Visit): String {
        val ref = restaurantsRef(coupleId).document()
        ref.set(restaurant.copy(id = ref.id).toMap()).await()
        addVisit(coupleId, ref.id, visit)
        return ref.id
    }

    suspend fun findRestaurantByName(coupleId: String, name: String, city: String): Restaurant? {
        val snapshot = restaurantsRef(coupleId).get().await()
        return snapshot.documents
            .map { doc -> Restaurant.fromMap(doc.id, doc.data ?: emptyMap()) }
            .find { it.name.equals(name, ignoreCase = true) && it.city.equals(city, ignoreCase = true) }
    }

    suspend fun addVisit(coupleId: String, restaurantId: String, visit: Visit): String {
        val ref = visitsRef(coupleId, restaurantId).document()
        ref.set(visit.copy(id = ref.id).toMap()).await()
        return ref.id
    }

    suspend fun updateRestaurant(coupleId: String, restaurant: Restaurant) {
        restaurantsRef(coupleId).document(restaurant.id).set(restaurant.toMap()).await()
    }

    suspend fun updateVisit(coupleId: String, restaurantId: String, visit: Visit) {
        visitsRef(coupleId, restaurantId).document(visit.id).set(visit.toMap()).await()
    }

    suspend fun deleteRestaurant(coupleId: String, restaurantId: String) {
        val visits = visitsRef(coupleId, restaurantId).get().await()
        visits.documents.forEach { it.reference.delete().await() }
        restaurantsRef(coupleId).document(restaurantId).delete().await()
    }

    fun wishlistFlow(coupleId: String): Flow<List<WishlistItem>> = callbackFlow {
        if (coupleId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = wishlistRef(coupleId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val items = snapshot?.documents?.map { doc ->
                    WishlistItem.fromMap(doc.id, doc.data ?: emptyMap())
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addWishlistItem(coupleId: String, item: WishlistItem): String {
        val ref = wishlistRef(coupleId).document()
        ref.set(item.copy(id = ref.id).toMap()).await()
        return ref.id
    }

    suspend fun deleteWishlistItem(coupleId: String, itemId: String) {
        wishlistRef(coupleId).document(itemId).delete().await()
    }

    suspend fun getRestaurantWithVisits(coupleId: String, restaurantId: String): Restaurant? {
        val doc = restaurantsRef(coupleId).document(restaurantId).get().await()
        if (!doc.exists()) return null
        val visits = visitsRef(coupleId, restaurantId)
            .orderBy("date", Query.Direction.DESCENDING)
            .get().await()
            .documents.map { vDoc -> Visit.fromMap(vDoc.id, vDoc.data ?: emptyMap()) }
        return Restaurant.fromMap(doc.id, doc.data ?: emptyMap(), visits)
    }
}
