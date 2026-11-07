package com.example.darhoney.data.model

data class PartnerNotification(
    val id: String = "",
    val type: String = "",
    val targetUserId: String = "",
    val fromUserId: String = "",
    val fromDisplayName: String = "",
    val message: String = "",
    val coupleId: String = "",
    val createdAt: Long = 0L,
    val read: Boolean = false
) {
    companion object {
        const val TYPE_PASSWORD_RESET = "password_reset"
        const val TYPE_WISHLIST_ADDED = "wishlist_added"
        const val TYPE_RESTAURANT_ADDED = "restaurant_added"
        const val TYPE_VISIT_ADDED = "visit_added"
    }
}
