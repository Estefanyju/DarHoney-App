package com.example.darhoney.data.model

data class Ratings(
    val food: Float = 0f,
    val service: Float = 0f,
    val ambiance: Float = 0f,
    val valueForMoney: Float = 0f,
    val cleanliness: Float = 0f,
    val speed: Float = 0f
) {
    fun average(): Float {
        val values = listOf(food, service, ambiance, valueForMoney, cleanliness, speed)
            .filter { it > 0f }
        return if (values.isEmpty()) 0f else values.average().toFloat()
    }

    fun toMap(): Map<String, Float> = mapOf(
        "food" to food,
        "service" to service,
        "ambiance" to ambiance,
        "valueForMoney" to valueForMoney,
        "cleanliness" to cleanliness,
        "speed" to speed
    )

    companion object {
        fun fromMap(map: Map<String, Any>?): Ratings {
            if (map == null) return Ratings()
            return Ratings(
                food = (map["food"] as? Number)?.toFloat() ?: 0f,
                service = (map["service"] as? Number)?.toFloat() ?: 0f,
                ambiance = (map["ambiance"] as? Number)?.toFloat() ?: 0f,
                valueForMoney = (map["valueForMoney"] as? Number)?.toFloat() ?: 0f,
                cleanliness = (map["cleanliness"] as? Number)?.toFloat() ?: 0f,
                speed = (map["speed"] as? Number)?.toFloat() ?: 0f
            )
        }
    }
}

data class PriceInfo(
    val averagePerPerson: Double = 0.0,
    val totalBill: Double = 0.0,
    val numberOfPeople: Int = 2,
    val priceRange: String = PriceRange.MEDIUM.name
) {
    fun toMap(): Map<String, Any> = mapOf(
        "averagePerPerson" to averagePerPerson,
        "totalBill" to totalBill,
        "numberOfPeople" to numberOfPeople,
        "priceRange" to priceRange
    )

    companion object {
        fun fromMap(map: Map<String, Any>?): PriceInfo {
            if (map == null) return PriceInfo()
            return PriceInfo(
                averagePerPerson = (map["averagePerPerson"] as? Number)?.toDouble() ?: 0.0,
                totalBill = (map["totalBill"] as? Number)?.toDouble() ?: 0.0,
                numberOfPeople = (map["numberOfPeople"] as? Number)?.toInt() ?: 2,
                priceRange = map["priceRange"] as? String ?: PriceRange.MEDIUM.name
            )
        }
    }
}
