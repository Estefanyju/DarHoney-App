package com.example.darhoney.data.model

data class WishlistItem(
    val id: String = "",
    val name: String = "",
    val city: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val link: String = "",
    val reason: String = "",
    val photoUrls: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "city" to city,
        "address" to address,
        "latitude" to latitude,
        "longitude" to longitude,
        "link" to link,
        "reason" to reason,
        "photoUrls" to photoUrls,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any>): WishlistItem = WishlistItem(
            id = id,
            name = map["name"] as? String ?: "",
            city = map["city"] as? String ?: "",
            address = map["address"] as? String ?: "",
            latitude = (map["latitude"] as? Number)?.toDouble() ?: 0.0,
            longitude = (map["longitude"] as? Number)?.toDouble() ?: 0.0,
            link = map["link"] as? String ?: "",
            reason = map["reason"] as? String ?: "",
            photoUrls = (map["photoUrls"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }
}
