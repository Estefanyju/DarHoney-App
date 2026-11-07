package com.example.darhoney.ui.restaurant

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.lifecycleScope
import com.example.darhoney.R
import com.example.darhoney.data.model.FavoriteTag
import com.example.darhoney.data.model.FoodType
import com.example.darhoney.data.model.PriceInfo
import com.example.darhoney.data.model.PriceRange
import com.example.darhoney.data.model.Ratings
import com.example.darhoney.data.model.Restaurant
import com.example.darhoney.data.model.Visit
import com.example.darhoney.data.repository.RestaurantRepository
import androidx.core.content.FileProvider
import com.example.darhoney.data.repository.PhotoStorageRepository
import com.example.darhoney.data.repository.PartnerNotificationRepository
import com.example.darhoney.databinding.ActivityAddRestaurantBinding
import com.example.darhoney.notifications.NotificationHelper
import com.example.darhoney.utils.GeocodingUtils
import com.example.darhoney.utils.LocationHelper
import com.example.darhoney.utils.PartnerNotifyHelper
import com.example.darhoney.utils.toUserMessage
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.io.File

class AddRestaurantActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddRestaurantBinding
    private val repository = RestaurantRepository()
    private val photoStorage = PhotoStorageRepository()
    private val partnerNotifications = PartnerNotificationRepository()
    private var coupleId: String = ""
    private var existingRestaurantId: String? = null
    private var wishlistId: String? = null
    private val selectedTags = mutableSetOf<String>()
    private val photoUris = mutableListOf<Uri>()
    private var cameraPhotoUri: Uri? = null
    private var pendingLatitude: Double? = null
    private var pendingLongitude: Double? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            fillCurrentLocation()
        } else {
            Toast.makeText(this, R.string.location_permission_required, Toast.LENGTH_SHORT).show()
        }
    }

    private val photoPicker = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val remaining = MAX_PHOTOS - photoUris.size
        if (remaining <= 0) {
            Toast.makeText(this, "Máximo $MAX_PHOTOS fotos por visita", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        photoUris.addAll(uris.take(remaining))
        binding.tvPhotoCount.text = "${photoUris.size} fotos seleccionadas"
    }

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            cameraPhotoUri?.let { uri ->
                if (photoUris.size < MAX_PHOTOS) {
                    photoUris.add(uri)
                    binding.tvPhotoCount.text = "${photoUris.size} fotos seleccionadas"
                }
            }
        }
        cameraPhotoUri = null
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera() else {
            Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddRestaurantBinding.inflate(layoutInflater)
        setContentView(binding.root)

        coupleId = intent.getStringExtra(EXTRA_COUPLE_ID) ?: ""
        wishlistId = intent.getStringExtra(EXTRA_WISHLIST_ID)
        existingRestaurantId = intent.getStringExtra(EXTRA_RESTAURANT_ID)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = if (existingRestaurantId != null) "Nueva visita" else "Nuevo restaurante"
        }

        binding.etName.setText(intent.getStringExtra(EXTRA_WISHLIST_NAME) ?: "")
        binding.etCity.setText(intent.getStringExtra(EXTRA_WISHLIST_CITY) ?: "")
        binding.etAddress.setText(intent.getStringExtra(EXTRA_WISHLIST_ADDRESS) ?: "")
        intent.getDoubleExtra(EXTRA_WISHLIST_LAT, 0.0).takeIf { it != 0.0 }?.let { pendingLatitude = it }
        intent.getDoubleExtra(EXTRA_WISHLIST_LNG, 0.0).takeIf { it != 0.0 }?.let { pendingLongitude = it }

        if (existingRestaurantId != null) {
            loadExistingRestaurant()
            binding.tilAddress.visibility = View.GONE
            binding.btnUseLocation.visibility = View.GONE
        }

        setupSpinners()
        setupTags()
        setupRatingBars()
        setupListeners()
    }

    private fun setupSpinners() {
        val foodTypes = FoodType.entries.map { it.displayName }
        setupDropdown(binding.actvFoodType, foodTypes)

        val priceRanges = PriceRange.entries.map { "${it.symbol} ${it.displayName}" }
        setupDropdown(binding.actvPriceRange, priceRanges)
    }

    private fun setupDropdown(view: AutoCompleteTextView, items: List<String>) {
        val adapter = ArrayAdapter(this, R.layout.item_spinner, items)
        view.setAdapter(adapter)
        if (items.isNotEmpty()) {
            view.setText(items.first(), false)
        }
    }

    private fun setupTags() {
        binding.chipGroupTags.removeAllViews()
        FavoriteTag.entries.forEach { tag ->
            val chip = Chip(this).apply {
                text = "${tag.emoji} ${tag.displayName}"
                isCheckable = true
                setTextColor(ContextCompat.getColorStateList(this@AddRestaurantActivity, R.color.chip_text))
                chipBackgroundColor = ContextCompat.getColorStateList(this@AddRestaurantActivity, R.color.chip_background)
                chipStrokeColor = ContextCompat.getColorStateList(this@AddRestaurantActivity, R.color.chip_stroke)
                chipStrokeWidth = resources.displayMetrics.density
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) selectedTags.add(tag.name) else selectedTags.remove(tag.name)
                }
            }
            binding.chipGroupTags.addView(chip)
        }
    }

    private fun setupRatingBars() {
        val ratingBars = listOf(
            binding.ratingFood,
            binding.ratingService,
            binding.ratingAmbiance,
            binding.ratingValue,
            binding.ratingCleanliness,
            binding.ratingSpeed
        )
        ratingBars.forEach { ratingBar ->
            ratingBar.setIsIndicator(false)
            val container = ratingBar.parent as ViewGroup
            val row = container.parent as ViewGroup
            val label = row.getChildAt(0)
            container.doOnLayout {
                val intrinsicW = ratingBar.width.toFloat()
                val intrinsicH = ratingBar.height.toFloat()
                if (intrinsicW <= 0f) return@doOnLayout

                val containerW = container.width.toFloat()
                val labelEnd = label.right.toFloat()
                val rowEnd = row.width - row.paddingEnd.toFloat()
                val emptySpace = containerW - intrinsicW
                val targetStart = labelEnd + emptySpace / 2f
                val targetWidth = rowEnd - targetStart
                val scale = (targetWidth / intrinsicW).coerceIn(1f, MAX_STAR_SCALE)

                ratingBar.pivotX = intrinsicW
                ratingBar.pivotY = intrinsicH / 2f
                ratingBar.scaleX = scale
                ratingBar.scaleY = scale
            }
            ratingBar.setOnTouchListener { view, event ->
                view.parent.requestDisallowInterceptTouchEvent(true)
                if (event.action == android.view.MotionEvent.ACTION_UP) {
                    view.parent.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
        }
    }

    private fun setupListeners() {
        binding.btnAddPhotos.setOnClickListener { photoPicker.launch("image/*") }
        binding.btnTakePhoto.setOnClickListener { requestCameraPhoto() }
        binding.btnSave.setOnClickListener { saveRestaurant() }
        binding.btnUseLocation.setOnClickListener { requestCurrentLocation() }
    }

    private fun requestCameraPhoto() {
        if (photoUris.size >= MAX_PHOTOS) {
            Toast.makeText(this, "Máximo $MAX_PHOTOS fotos por visita", Toast.LENGTH_SHORT).show()
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            launchCamera()
        }
    }

    private fun launchCamera() {
        val file = File(cacheDir, "visit_${System.currentTimeMillis()}.jpg")
        cameraPhotoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
        takePicture.launch(requireNotNull(cameraPhotoUri))
    }

    private fun requestCurrentLocation() {
        if (hasLocationPermission()) {
            fillCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun fillCurrentLocation() {
        Toast.makeText(this, R.string.location_loading, Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val details = LocationHelper.getCurrentLocationDetails(this@AddRestaurantActivity)
            if (details == null) {
                Toast.makeText(this@AddRestaurantActivity, R.string.location_not_available, Toast.LENGTH_SHORT).show()
                return@launch
            }
            pendingLatitude = details.latitude
            pendingLongitude = details.longitude
            binding.etAddress.setText(details.address)
            if (binding.etCity.text.isNullOrBlank()) {
                binding.etCity.setText(details.city.ifBlank { "Bogotá" })
            }
            Toast.makeText(this@AddRestaurantActivity, "Ubicación agregada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadExistingRestaurant() {
        lifecycleScope.launch {
            val restaurant = repository.getRestaurantWithVisits(coupleId, existingRestaurantId!!) ?: return@launch
            binding.etName.setText(restaurant.name)
            binding.etCity.setText(restaurant.city)
            binding.etName.isEnabled = false
            binding.etCity.isEnabled = false
        }
    }

    private fun saveRestaurant() {
        val name = binding.etName.text.toString().trim()
        val city = binding.etCity.text.toString().trim()

        binding.tilName.error = null
        if (name.isBlank()) {
            binding.tilName.error = "Ingresa el nombre del restaurante"
            return
        }
        if (city.isBlank()) {
            Toast.makeText(this, "Ingresa la ciudad", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false

        lifecycleScope.launch {
            try {
                val photoUrls = uploadPhotos()

                val ratings = Ratings(
                    food = binding.ratingFood.rating,
                    service = binding.ratingService.rating,
                    ambiance = binding.ratingAmbiance.rating,
                    valueForMoney = binding.ratingValue.rating,
                    cleanliness = binding.ratingCleanliness.rating,
                    speed = binding.ratingSpeed.rating
                )

                val priceInfo = PriceInfo(
                    averagePerPerson = binding.etAvgPrice.text.toString().toDoubleOrNull() ?: 0.0,
                    totalBill = binding.etTotalBill.text.toString().toDoubleOrNull() ?: 0.0,
                    numberOfPeople = binding.etPeople.text.toString().toIntOrNull() ?: 2,
                    priceRange = PriceRange.entries.firstOrNull {
                        "${it.symbol} ${it.displayName}" == binding.actvPriceRange.text.toString()
                    }?.name ?: PriceRange.MEDIUM.name
                )

                val visit = Visit(
                    date = System.currentTimeMillis(),
                    comments = binding.etComments.text.toString().trim(),
                    memories = binding.etMemories.text.toString().trim(),
                    photoUrls = photoUrls,
                    ratings = ratings,
                    priceInfo = priceInfo
                )

                val existing = existingRestaurantId?.let { id ->
                    repository.getRestaurantWithVisits(coupleId, id)
                } ?: repository.findRestaurantByName(coupleId, name, city)

                if (existing != null) {
                    repository.addVisit(coupleId, existing.id, visit)
                    var updated = existing
                    if (selectedTags.isNotEmpty()) {
                        updated = updated.copy(
                            favoriteTags = (existing.favoriteTags + selectedTags).distinct()
                        )
                    }
                    if (updated.coverPhotoUrl.isBlank() && photoUrls.isNotEmpty()) {
                        updated = updated.copy(coverPhotoUrl = photoUrls.first())
                    }
                    if (updated != existing) {
                        repository.updateRestaurant(coupleId, updated)
                    }
                    Toast.makeText(this@AddRestaurantActivity, "Nueva visita agregada", Toast.LENGTH_SHORT).show()
                    notifyPartnerAboutRestaurant(existing.name, isNewVisit = true)
                } else {
                    val foodType = FoodType.entries.firstOrNull {
                        it.displayName == binding.actvFoodType.text.toString()
                    }?.name ?: FoodType.OTRO.name
                    val address = binding.etAddress.text.toString().trim()
                    val coords = pendingLatitude?.let { lat ->
                        pendingLongitude?.let { lng -> lat to lng }
                    } ?: GeocodingUtils.resolveCoordinates(
                        this@AddRestaurantActivity,
                        name,
                        address,
                        city
                    )
                    val restaurant = Restaurant(
                        name = name,
                        address = address,
                        city = city,
                        foodType = foodType,
                        latitude = coords?.first ?: 0.0,
                        longitude = coords?.second ?: 0.0,
                        coverPhotoUrl = photoUrls.firstOrNull() ?: "",
                        favoriteTags = selectedTags.toList()
                    )
                    repository.addRestaurant(coupleId, restaurant, visit)
                    val locationMessage = when {
                        coords != null -> "Restaurante registrado y ubicado en el mapa"
                        address.isNotBlank() -> "Restaurante registrado (no se pudo ubicar la dirección)"
                        else -> "Restaurante registrado"
                    }
                    Toast.makeText(this@AddRestaurantActivity, locationMessage, Toast.LENGTH_SHORT).show()
                    notifyPartnerAboutRestaurant(name, isNewVisit = false)
                }

                wishlistId?.let { repository.deleteWishlistItem(coupleId, it) }
                NotificationHelper.showTaskSaved(
                    this@AddRestaurantActivity,
                    coupleId,
                    getString(R.string.app_name),
                    if (existing != null) "Nueva visita registrada" else "Restaurante guardado en la nube"
                )
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@AddRestaurantActivity, e.toUserMessage(), Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true
            }
        }
    }

    private suspend fun uploadPhotos(): List<String> {
        if (photoUris.isEmpty()) return emptyList()

        binding.progressUpload.visibility = View.VISIBLE
        binding.tvUploadProgress.visibility = View.VISIBLE
        binding.progressUpload.progress = 0

        val uploaded = mutableListOf<String>()
        var failed = 0
        photoUris.forEachIndexed { index, uri ->
            try {
                val url = photoStorage.uploadPhoto(coupleId, uri) { pct ->
                    runOnUiThread {
                        binding.progressUpload.progress = pct
                        binding.tvUploadProgress.text =
                            "Subiendo foto ${index + 1}/${photoUris.size}: $pct%"
                    }
                }
                uploaded.add(url)
            } catch (_: Exception) {
                failed++
            }
        }

        binding.progressUpload.visibility = View.GONE
        binding.tvUploadProgress.visibility = View.GONE

        if (failed > 0) {
            val message = if (uploaded.isEmpty()) {
                "No se pudieron subir las fotos. Revisa tu conexión."
            } else {
                "$failed foto(s) no se subieron. Se usarán ${uploaded.size} foto(s)."
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
        return uploaded
    }

    private suspend fun notifyPartnerAboutRestaurant(restaurantName: String, isNewVisit: Boolean) {
        runCatching {
            val (uid, actorName) = PartnerNotifyHelper.currentActor() ?: return@runCatching
            if (isNewVisit) {
                partnerNotifications.notifyVisitAdded(coupleId, uid, actorName, restaurantName)
            } else {
                partnerNotifications.notifyRestaurantAdded(coupleId, uid, actorName, restaurantName)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        const val EXTRA_COUPLE_ID = "couple_id"
        const val EXTRA_RESTAURANT_ID = "restaurant_id"
        const val EXTRA_WISHLIST_NAME = "wishlist_name"
        const val EXTRA_WISHLIST_CITY = "wishlist_city"
        const val EXTRA_WISHLIST_ADDRESS = "wishlist_address"
        const val EXTRA_WISHLIST_LAT = "wishlist_lat"
        const val EXTRA_WISHLIST_LNG = "wishlist_lng"
        const val EXTRA_WISHLIST_ID = "wishlist_id"
        private const val MAX_PHOTOS = 15
        private const val MAX_STAR_SCALE = 2.2f
    }
}
