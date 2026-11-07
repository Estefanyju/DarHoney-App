package com.example.darhoney.data.model

data class Restaurant(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val city: String = "",
    val foodType: String = FoodType.OTRO.name,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val coverPhotoUrl: String = "",
    val favoriteTags: List<String> = emptyList(),
    val visits: List<Visit> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    val latestVisit: Visit? get() = visits.maxByOrNull { it.date }
    val visitCount: Int get() = visits.size
    val averageRating: Float get() {
        val ratings = visits.map { it.ratings.average() }.filter { it > 0f }
        return if (ratings.isEmpty()) 0f else ratings.average().toFloat()
    }
    val totalSpent: Double get() = visits.sumOf { it.priceInfo.totalBill }
    val allPhotos: List<String> get() = visits.flatMap { it.photoUrls }
    val displayCoverPhoto: String?
        get() = coverPhotoUrl.takeIf { it.isNotBlank() } ?: allPhotos.firstOrNull()

    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "address" to address,
        "city" to city,
        "foodType" to foodType,
        "latitude" to latitude,
        "longitude" to longitude,
        "coverPhotoUrl" to coverPhotoUrl,
        "favoriteTags" to favoriteTags,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any>, visits: List<Visit> = emptyList()): Restaurant =
            Restaurant(
                id = id,
                name = map["name"] as? String ?: "",
                address = map["address"] as? String ?: "",
                city = map["city"] as? String ?: "",
                foodType = map["foodType"] as? String ?: FoodType.OTRO.name,
                latitude = (map["latitude"] as? Number)?.toDouble() ?: 0.0,
                longitude = (map["longitude"] as? Number)?.toDouble() ?: 0.0,
                coverPhotoUrl = map["coverPhotoUrl"] as? String ?: "",
                favoriteTags = (map["favoriteTags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                visits = visits,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
    }
}
