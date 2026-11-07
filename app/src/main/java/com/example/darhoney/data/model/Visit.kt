package com.example.darhoney.data.model

data class Visit(
    val id: String = "",
    val date: Long = System.currentTimeMillis(),
    val comments: String = "",
    val memories: String = "",
    val photoUrls: List<String> = emptyList(),
    val ratings: Ratings = Ratings(),
    val priceInfo: PriceInfo = PriceInfo(),
    val createdBy: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "date" to date,
        "comments" to comments,
        "memories" to memories,
        "photoUrls" to photoUrls,
        "ratings" to ratings.toMap(),
        "priceInfo" to priceInfo.toMap(),
        "createdBy" to createdBy
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any>): Visit = Visit(
            id = id,
            date = (map["date"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            comments = map["comments"] as? String ?: "",
            memories = map["memories"] as? String ?: "",
            photoUrls = (map["photoUrls"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            ratings = Ratings.fromMap(map["ratings"].toStringAnyMap()),
            priceInfo = PriceInfo.fromMap(map["priceInfo"].toStringAnyMap()),
            createdBy = map["createdBy"] as? String ?: ""
        )

        private fun Any?.toStringAnyMap(): Map<String, Any>? {
            val raw = this as? Map<*, *> ?: return null
            return buildMap {
                for ((key, value) in raw) {
                    if (key is String && value != null) put(key, value)
                }
            }
        }
    }
}
