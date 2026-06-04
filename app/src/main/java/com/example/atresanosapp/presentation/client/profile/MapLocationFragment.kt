package com.example.atresanosapp.presentation.client.profile

import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.atresanosapp.R
import com.example.atresanosapp.databinding.FragmentMapLocationBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class MapLocationFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentMapLocationBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null
    private var selectedMarker: Marker? = null
    private var latitudSeleccionada: Double = 0.0
    private var longitudSeleccionada: Double = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Read initial arguments
        latitudSeleccionada = arguments?.getDouble("latitud") ?: 0.0
        longitudSeleccionada = arguments?.getDouble("longitud") ?: 0.0

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSearch.setOnClickListener {
            searchAddress(binding.etSearchAddress.text.toString())
        }

        binding.etSearchAddress.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchAddress(binding.etSearchAddress.text.toString())
                true
            } else {
                false
            }
        }

        binding.btnConfirmLocation.setOnClickListener {
            if (latitudSeleccionada == 0.0 && longitudSeleccionada == 0.0) {
                Toast.makeText(context, "Por favor selecciona una ubicación en el mapa", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val bundle = Bundle().apply {
                putDouble("latitud", latitudSeleccionada)
                putDouble("longitud", longitudSeleccionada)
            }
            parentFragmentManager.setFragmentResult("locationResult", bundle)
            findNavController().navigateUp()
        }
    }

    private fun searchAddress(addressText: String) {
        if (addressText.isBlank()) return

        val geocoder = Geocoder(requireContext())
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val addresses = geocoder.getFromLocationName(addressText, 1)
                withContext(Dispatchers.Main) {
                    if (!addresses.isNullOrEmpty()) {
                        val location = addresses[0]
                        latitudSeleccionada = location.latitude
                        longitudSeleccionada = location.longitude
                        updateMapMarker(true)
                    } else {
                        Toast.makeText(context, "Dirección no encontrada", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error al buscar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        googleMap?.setOnMapClickListener { latLng ->
            latitudSeleccionada = latLng.latitude
            longitudSeleccionada = latLng.longitude
            updateMapMarker(false)
        }
        
        // Initial setup
        if (latitudSeleccionada != 0.0 && longitudSeleccionada != 0.0) {
            updateMapMarker(true)
        } else {
            // Default CDMX
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(19.4326, -99.1332), 5f))
        }
    }

    private fun updateMapMarker(animate: Boolean) {
        if (googleMap == null) return

        val location = LatLng(latitudSeleccionada, longitudSeleccionada)
        selectedMarker?.remove()
        selectedMarker = googleMap?.addMarker(MarkerOptions().position(location).title("Mi Ubicación"))
        
        if (animate) {
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 16f))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
