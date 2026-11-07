package com.example.darhoney.data.model

data class Couple(
    val id: String = "",
    val name: String = "Nuestra aventura gastronómica",
    val memberIds: List<String> = emptyList(),
    val inviteCode: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val coupleId: String = "",
    val role: String = UserRole.STANDARD.name
) {
    fun userRole(): UserRole = UserRole.fromString(role)
    fun isAdmin(): Boolean = userRole() == UserRole.ADMIN
}
