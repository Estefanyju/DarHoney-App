package com.example.darhoney.ui.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.darhoney.R
import com.example.darhoney.databinding.FragmentMapBinding
import com.example.darhoney.data.model.Restaurant
import com.example.darhoney.data.model.WishlistItem
import com.example.darhoney.ui.MainViewModel
import com.example.darhoney.ui.ViewModelFactory
import com.example.darhoney.utils.FormatUtils
import com.example.darhoney.utils.LocationHelper
import com.example.darhoney.utils.MapDefaults
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels { ViewModelFactory.instance }
    private var googleMap: GoogleMap? = null
    private var coupleId: String = ""
    private var satelliteMode = false
    private val markerRestaurantMap = mutableMapOf<Marker, Restaurant>()
    private val markerWishlistMap = mutableMapOf<Marker, WishlistItem>()
    private var latestRestaurants = emptyList<Restaurant>()
    private var latestWishlist = emptyList<WishlistItem>()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            enableMyLocation()
            centerOnBestLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        coupleId = arguments?.getString(ARG_COUPLE_ID) ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        viewModel.restaurants.observe(viewLifecycleOwner) { restaurants ->
            latestRestaurants = restaurants
            updateMarkers()
        }

        viewModel.wishlist.observe(viewLifecycleOwner) { items ->
            latestWishlist = items
            updateMarkers()
        }

        binding.btnMapType.setOnClickListener { toggleMapType() }
    }

    private fun toggleMapType() {
        val map = googleMap ?: return
        satelliteMode = !satelliteMode
        map.mapType = if (satelliteMode) GoogleMap.MAP_TYPE_SATELLITE else GoogleMap.MAP_TYPE_NORMAL
        binding.btnMapType.text = if (satelliteMode) "Normal" else "Satélite"
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true

        val bogota = LatLng(MapDefaults.BOGOTA.first, MapDefaults.BOGOTA.second)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(bogota, MapDefaults.DEFAULT_ZOOM))

        map.setOnMarkerClickListener { marker ->
            marker.showInfoWindow()
            markerRestaurantMap[marker]?.let { return@setOnMarkerClickListener true }
            markerWishlistMap[marker]?.let { return@setOnMarkerClickListener true }
            true
        }

        requestLocationAccess()
        updateMarkers()
    }

    private fun requestLocationAccess() {
        if (hasLocationPermission()) {
            enableMyLocation()
            centerOnBestLocation()
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
        val fine = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun enableMyLocation() {
        if (!hasLocationPermission()) return
        try {
            googleMap?.isMyLocationEnabled = true
        } catch (_: SecurityException) {
        }
    }

    private fun centerOnBestLocation() {
        lifecycleScope.launch {
            val map = googleMap ?: return@launch
            val restaurantPoints = latestRestaurants
                .filter { it.latitude != 0.0 && it.longitude != 0.0 }
                .map { LatLng(it.latitude, it.longitude) }
            val wishlistPoints = latestWishlist
                .filter { it.latitude != 0.0 && it.longitude != 0.0 }
                .map { LatLng(it.latitude, it.longitude) }

            val allPoints = restaurantPoints + wishlistPoints
            if (allPoints.isNotEmpty()) {
                val builder = LatLngBounds.builder()
                allPoints.forEach { builder.include(it) }
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 120))
                return@launch
            }

            val current = if (hasLocationPermission()) {
                LocationHelper.getCurrentLocationDetails(requireContext())
            } else {
                null
            }

            val target = current?.let { LatLng(it.latitude, it.longitude) }
                ?: LatLng(MapDefaults.BOGOTA.first, MapDefaults.BOGOTA.second)
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(target, MapDefaults.DEFAULT_ZOOM))
        }
    }

    private fun updateMarkers() {
        val map = googleMap ?: return
        map.clear()
        markerRestaurantMap.clear()
        markerWishlistMap.clear()

        latestRestaurants.filter { it.latitude != 0.0 && it.longitude != 0.0 }.forEach { restaurant ->
            val position = LatLng(restaurant.latitude, restaurant.longitude)
            val marker = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(restaurant.name)
                    .snippet("${restaurant.city} · ${FormatUtils.formatRating(restaurant.averageRating)} ★")
                    .icon(
                        BitmapDescriptorFactory.defaultMarker(
                            if (restaurant.averageRating >= 4f) BitmapDescriptorFactory.HUE_ORANGE
                            else BitmapDescriptorFactory.HUE_RED
                        )
                    )
            )
            marker?.let { markerRestaurantMap[it] = restaurant }
        }

        latestWishlist.filter { it.latitude != 0.0 && it.longitude != 0.0 }.forEach { item ->
            val position = LatLng(item.latitude, item.longitude)
            val marker = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title("✨ ${item.name}")
                    .snippet(item.city.ifBlank { "Lista de deseos" })
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
            )
            marker?.let { markerWishlistMap[it] = item }
        }
    }

    private fun showRestaurantInfo(restaurant: Restaurant) {
        val visit = restaurant.latestVisit
        val message = buildString {
            append("📍 ${restaurant.address}\n")
            append("⭐ ${FormatUtils.formatRating(restaurant.averageRating)}\n")
            visit?.let {
                append("📅 ${FormatUtils.formatDate(it.date)}\n")
                if (it.priceInfo.totalBill > 0) append("💰 ${FormatUtils.formatCurrency(it.priceInfo.totalBill)}\n")
                if (it.comments.isNotBlank()) append("\n${it.comments}")
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(restaurant.name)
            .setMessage(message)
            .setPositiveButton("Ver detalle") { _, _ ->
                startActivity(android.content.Intent(requireContext(), com.example.darhoney.ui.restaurant.RestaurantDetailActivity::class.java).apply {
                    putExtra(com.example.darhoney.ui.restaurant.RestaurantDetailActivity.EXTRA_COUPLE_ID, coupleId)
                    putExtra(com.example.darhoney.ui.restaurant.RestaurantDetailActivity.EXTRA_RESTAURANT_ID, restaurant.id)
                })
            }
            .setNeutralButton("Google Maps") { _, _ ->
                val uri = android.net.Uri.parse("geo:${restaurant.latitude},${restaurant.longitude}?q=${restaurant.name}")
                startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
            }
            .show()
    }

    private fun showWishlistInfo(item: WishlistItem) {
        val message = buildString {
            if (item.address.isNotBlank()) append("📍 ${item.address}\n")
            if (item.city.isNotBlank()) append("🏙️ ${item.city}\n")
            if (item.reason.isNotBlank()) append("\n${item.reason}")
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(item.name)
            .setMessage(message.ifBlank { "Restaurante en lista de deseos" })
            .setPositiveButton("Google Maps") { _, _ ->
                val query = listOf(item.name, item.address, item.city).filter { it.isNotBlank() }.joinToString(", ")
                val uri = android.net.Uri.parse("geo:${item.latitude},${item.longitude}?q=${android.net.Uri.encode(query)}")
                startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_COUPLE_ID = "couple_id"
        fun newInstance(coupleId: String) = MapFragment().apply {
            arguments = Bundle().apply { putString(ARG_COUPLE_ID, coupleId) }
        }
    }
}
