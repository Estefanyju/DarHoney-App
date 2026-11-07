package com.example.darhoney.utils

import com.example.darhoney.data.model.FavoriteTag
import com.example.darhoney.data.model.FoodType
import com.example.darhoney.data.model.PriceRange
import com.example.darhoney.data.model.Restaurant
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("es-ES"))
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX"))

    fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))
    fun formatCurrency(amount: Double): String = currencyFormat.format(amount)

    fun formatRating(rating: Float): String =
        if (rating > 0f) String.format(Locale.getDefault(), "%.1f", rating) else "—"

    fun starsText(rating: Float): String {
        if (rating <= 0f) return "Sin calificar"
        val full = rating.toInt().coerceIn(0, 5)
        val half = rating - full >= 0.5f
        return "★".repeat(full) + (if (half) "½" else "") + "☆".repeat(5 - full - if (half) 1 else 0)
    }
}

object SearchFilter {
    fun filter(
        restaurants: List<Restaurant>,
        query: String = "",
        foodType: FoodType? = null,
        minRating: Float = 0f,
        priceRange: PriceRange? = null,
        favoriteTag: FavoriteTag? = null,
        city: String = ""
    ): List<Restaurant> {
        return restaurants.filter { restaurant ->
            val matchesQuery = query.isBlank() ||
                restaurant.name.contains(query, ignoreCase = true) ||
                restaurant.city.contains(query, ignoreCase = true) ||
                restaurant.address.contains(query, ignoreCase = true)

            val matchesFoodType = foodType == null || restaurant.foodType == foodType.name
            val matchesRating = restaurant.averageRating >= minRating
            val matchesPrice = priceRange == null ||
                restaurant.latestVisit?.priceInfo?.priceRange == priceRange.name
            val matchesTag = favoriteTag == null ||
                restaurant.favoriteTags.contains(favoriteTag.name)
            val matchesCity = city.isBlank() ||
                restaurant.city.contains(city, ignoreCase = true)

            matchesQuery && matchesFoodType && matchesRating && matchesPrice && matchesTag && matchesCity
        }
    }
}

object StatsCalculator {
    data class CoupleStats(
        val totalRestaurants: Int = 0,
        val totalVisits: Int = 0,
        val topCity: String = "—",
        val favoriteFoodType: String = "—",
        val bestRated: Restaurant? = null,
        val mostExpensive: Restaurant? = null,
        val cheapest: Restaurant? = null,
        val averageSpend: Double = 0.0,
        val totalSpent: Double = 0.0,
        val romanticDates: Int = 0,
        val mostVisited: Restaurant? = null
    )

    fun calculate(restaurants: List<Restaurant>): CoupleStats {
        if (restaurants.isEmpty()) return CoupleStats()

        val allVisits = restaurants.flatMap { r -> r.visits.map { r to it } }
        val totalSpent = allVisits.sumOf { it.second.priceInfo.totalBill }
        val visitsWithBill = allVisits.filter { it.second.priceInfo.totalBill > 0 }

        val cityCounts = restaurants.groupingBy { it.city }.eachCount()
        val foodTypeCounts = restaurants.groupingBy { it.foodType }.eachCount()

        val romanticDates = restaurants.count {
            it.favoriteTags.contains(FavoriteTag.ROMANTIC.name)
        }

        return CoupleStats(
            totalRestaurants = restaurants.size,
            totalVisits = allVisits.size,
            topCity = cityCounts.maxByOrNull { it.value }?.key ?: "—",
            favoriteFoodType = foodTypeCounts.maxByOrNull { it.value }?.let {
                FoodType.fromString(it.key).displayName
            } ?: "—",
            bestRated = restaurants.filter { it.averageRating > 0 }.maxByOrNull { it.averageRating },
            mostExpensive = restaurants.filter { it.latestVisit != null }.maxByOrNull {
                it.latestVisit?.priceInfo?.averagePerPerson ?: 0.0
            },
            cheapest = restaurants.filter {
                (it.latestVisit?.priceInfo?.averagePerPerson ?: 0.0) > 0
            }.minByOrNull { it.latestVisit?.priceInfo?.averagePerPerson ?: Double.MAX_VALUE },
            averageSpend = if (visitsWithBill.isEmpty()) 0.0
            else visitsWithBill.map { it.second.priceInfo.totalBill }.average(),
            totalSpent = totalSpent,
            romanticDates = romanticDates,
            mostVisited = restaurants.maxByOrNull { it.visitCount }
        )
    }
}

object RankingsCalculator {
    data class Ranking(val title: String, val emoji: String, val restaurants: List<Restaurant>)

    fun generate(restaurants: List<Restaurant>): List<Ranking> {
        val rated = restaurants.filter { it.averageRating > 0 }
            .sortedByDescending { it.averageRating }.take(10)

        return listOf(
            Ranking("Top 10 restaurantes", "🏆", rated),
            Ranking("Mejores pizzas", "🍕",
                restaurants.filter { it.foodType == FoodType.PIZZA.name }
                    .sortedByDescending { it.averageRating }.take(10)),
            Ranking("Mejor sushi", "🍣",
                restaurants.filter { it.foodType == FoodType.SUSHI.name || it.foodType == FoodType.JAPONESA.name }
                    .sortedByDescending { it.averageRating }.take(10)),
            Ranking("Mejores cafeterías", "☕",
                restaurants.filter { it.foodType == FoodType.CAFETERIA.name }
                    .sortedByDescending { it.averageRating }.take(10)),
            Ranking("Más económicos", "💸",
                restaurants.filter { (it.latestVisit?.priceInfo?.averagePerPerson ?: 0.0) > 0 }
                    .sortedBy { it.latestVisit?.priceInfo?.averagePerPerson ?: Double.MAX_VALUE }.take(10)),
            Ranking("Más costosos", "💰",
                restaurants.filter { (it.latestVisit?.priceInfo?.averagePerPerson ?: 0.0) > 0 }
                    .sortedByDescending { it.latestVisit?.priceInfo?.averagePerPerson ?: 0.0 }.take(10)),
            Ranking("Favoritos de la pareja", "❤️",
                restaurants.filter { it.favoriteTags.contains(FavoriteTag.FAVORITE.name) }
                    .sortedByDescending { it.averageRating })
        )
    }
}
