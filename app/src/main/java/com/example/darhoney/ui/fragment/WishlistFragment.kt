package com.example.darhoney.ui.fragment

import android.Manifest
import android.content.Intent
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
import com.example.darhoney.data.model.WishlistItem
import com.example.darhoney.data.repository.PartnerNotificationRepository
import com.example.darhoney.data.repository.RestaurantRepository
import com.example.darhoney.databinding.FragmentWishlistBinding
import com.example.darhoney.ui.MainViewModel
import com.example.darhoney.ui.ViewModelFactory
import com.example.darhoney.ui.adapter.WishlistAdapter
import com.example.darhoney.ui.restaurant.AddRestaurantActivity
import com.example.darhoney.utils.LocationHelper
import com.example.darhoney.utils.PartnerNotifyHelper
import com.example.darhoney.utils.applyStatusBarPadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class WishlistFragment : Fragment() {

    private var _binding: FragmentWishlistBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels { ViewModelFactory.instance }
    private val repository = RestaurantRepository()
    private val partnerNotifications = PartnerNotificationRepository()
    private lateinit var adapter: WishlistAdapter
    private var coupleId: String = ""

    private var pendingLatitude: Double? = null
    private var pendingLongitude: Double? = null
    private var pendingAddressField: TextInputEditText? = null
    private var pendingCityField: TextInputEditText? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            fillCurrentLocation()
        } else {
            Toast.makeText(requireContext(), R.string.location_permission_required, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        coupleId = arguments?.getString(ARG_COUPLE_ID) ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWishlistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.headerBar.applyStatusBarPadding()

        adapter = WishlistAdapter(
            onDelete = { item ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Eliminar")
                    .setMessage("¿Eliminar ${item.name} de la lista de deseos?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        lifecycleScope.launch {
                            repository.deleteWishlistItem(coupleId, item.id)
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            },
            onConvert = { item ->
                startActivity(Intent(requireContext(), AddRestaurantActivity::class.java).apply {
                    putExtra(AddRestaurantActivity.EXTRA_COUPLE_ID, coupleId)
                    putExtra(AddRestaurantActivity.EXTRA_WISHLIST_NAME, item.name)
                    putExtra(AddRestaurantActivity.EXTRA_WISHLIST_CITY, item.city)
                    putExtra(AddRestaurantActivity.EXTRA_WISHLIST_ADDRESS, item.address)
                    putExtra(AddRestaurantActivity.EXTRA_WISHLIST_LAT, item.latitude)
                    putExtra(AddRestaurantActivity.EXTRA_WISHLIST_LNG, item.longitude)
                    putExtra(AddRestaurantActivity.EXTRA_WISHLIST_ID, item.id)
                })
            }
        )
        binding.recyclerView.adapter = adapter

        viewModel.wishlist.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            binding.emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.fabAdd.setOnClickListener { showAddDialog() }
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_wishlist, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etName)
        val etCity = dialogView.findViewById<TextInputEditText>(R.id.etCity)
        val etAddress = dialogView.findViewById<TextInputEditText>(R.id.etAddress)
        val etLink = dialogView.findViewById<TextInputEditText>(R.id.etLink)
        val etReason = dialogView.findViewById<TextInputEditText>(R.id.etReason)
        val btnUseLocation = dialogView.findViewById<View>(R.id.btnUseLocation)

        pendingLatitude = null
        pendingLongitude = null

        btnUseLocation.setOnClickListener {
            pendingAddressField = etAddress
            pendingCityField = etCity
            requestCurrentLocation()
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Agregar a lista de deseos")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isBlank()) {
                    Toast.makeText(requireContext(), "Ingresa el nombre", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val address = etAddress.text.toString().trim()
                val city = etCity.text.toString().trim()
                lifecycleScope.launch {
                    var latitude = pendingLatitude ?: 0.0
                    var longitude = pendingLongitude ?: 0.0
                    if (latitude == 0.0 && longitude == 0.0 && (address.isNotBlank() || city.isNotBlank())) {
                        com.example.darhoney.utils.GeocodingUtils
                            .resolveCoordinates(requireContext(), name, address, city)
                            ?.let {
                                latitude = it.first
                                longitude = it.second
                            }
                    }
                    repository.addWishlistItem(
                        coupleId,
                        WishlistItem(
                            name = name,
                            city = city,
                            address = address,
                            latitude = latitude,
                            longitude = longitude,
                            link = etLink.text.toString().trim(),
                            reason = etReason.text.toString().trim()
                        )
                    )
                    runCatching {
                        val (uid, actorName) = PartnerNotifyHelper.currentActor() ?: return@runCatching
                        partnerNotifications.notifyWishlistAdded(coupleId, uid, actorName, name)
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
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

    private fun fillCurrentLocation() {
        val addressField = pendingAddressField ?: return
        val cityField = pendingCityField
        Toast.makeText(requireContext(), R.string.location_loading, Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val details = LocationHelper.getCurrentLocationDetails(requireContext())
            if (details == null) {
                Toast.makeText(requireContext(), R.string.location_not_available, Toast.LENGTH_SHORT).show()
                return@launch
            }
            pendingLatitude = details.latitude
            pendingLongitude = details.longitude
            addressField.setText(details.address)
            cityField?.setText(details.city.ifBlank { "Bogotá" })
            Toast.makeText(requireContext(), "Ubicación agregada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_COUPLE_ID = "couple_id"
        fun newInstance(coupleId: String) = WishlistFragment().apply {
            arguments = Bundle().apply { putString(ARG_COUPLE_ID, coupleId) }
        }
    }
}
