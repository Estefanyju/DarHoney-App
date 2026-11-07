package com.example.darhoney.data.model

enum class FoodType(val displayName: String) {
    ITALIANA("Italiana"),
    JAPONESA("Japonesa"),
    MEXICANA("Mexicana"),
    HAMBURGUESAS("Hamburguesas"),
    CAFETERIA("Cafetería"),
    PIZZA("Pizza"),
    SUSHI("Sushi"),
    CHINA("China"),
    INDIA("India"),
    MARISCOS("Mariscos"),
    VEGETARIANA("Vegetariana"),
    BBQ("BBQ"),
    TAPAS("Tapas"),
    BRUNCH("Brunch"),
    POSTRES("Postres"),
    OTRO("Otro");

    companion object {
        fun fromString(value: String): FoodType =
            entries.find { it.name == value || it.displayName == value } ?: OTRO
    }
}

enum class PriceRange(val displayName: String, val symbol: String) {
    VERY_CHEAP("Muy económico", "$"),
    CHEAP("Económico", "$$"),
    MEDIUM("Precio medio", "$$$"),
    EXPENSIVE("Costoso", "$$$$"),
    VERY_EXPENSIVE("Muy costoso", "$$$$$");

    companion object {
        fun fromString(value: String): PriceRange =
            entries.find { it.name == value } ?: MEDIUM
    }
}

enum class FavoriteTag(val displayName: String, val emoji: String) {
    FAVORITE("Favorito", "❤️"),
    SPECIAL("Lugar especial", "💍"),
    BIRTHDAY("Cumpleaños", "🎂"),
    ROMANTIC("Cita romántica", "💕"),
    WOULD_RETURN("Volvería", "🍔"),
    WOULD_NOT_RETURN("No volvería", "🥲");

    companion object {
        fun fromString(value: String): FavoriteTag? =
            entries.find { it.name == value }
    }
}

enum class UserRole(val displayName: String) {
    ADMIN("Administrador"),
    STANDARD("Estándar");

    companion object {
        fun fromString(value: String): UserRole =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: STANDARD

        fun fromEmail(email: String): UserRole =
            if (email.trim().lowercase().startsWith("admin@")) ADMIN else STANDARD
    }
}
