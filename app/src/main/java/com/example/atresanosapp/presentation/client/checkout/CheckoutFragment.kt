package com.example.atresanosapp.presentation.client.checkout

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.atresanosapp.R
import com.example.atresanosapp.databinding.FragmentCheckoutBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CheckoutFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null
    private var selectedDate: Calendar = Calendar.getInstance()

    private var dateSelected = false
    private var timeSelected = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val cartViewModel: com.example.atresanosapp.presentation.client.cart.CartViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        binding.btnSelectDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnSelectTime.setOnClickListener {
            showTimePicker()
        }

        binding.tilSearch.setEndIconOnClickListener {
            searchAddress(binding.etSearch.text.toString())
        }

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                searchAddress(binding.etSearch.text.toString())
                true
            } else {
                false
            }
        }

        lifecycleScope.launchWhenStarted {
            cartViewModel.cartState.collect { state ->
                when (state) {
                    is com.example.atresanosapp.presentation.client.cart.CartState.Processing -> {
                        binding.btnConfirmOrder.isEnabled = false
                        binding.btnConfirmOrder.text = "Procesando..."
                    }
                    is com.example.atresanosapp.presentation.client.cart.CartState.Success -> {
                        Toast.makeText(context, "Pedido Confirmado. ID: ${state.idPedido}", Toast.LENGTH_LONG).show()
                        com.example.atresanosapp.utils.NotificationHelper.scheduleDeliveryReminder(
                            requireContext(), 
                            state.idPedido, 
                            selectedDate.timeInMillis
                        )
                        com.example.atresanosapp.utils.NotificationHelper.showNotification(
                            requireContext(),
                            "Pedido Confirmado",
                            "Tu pedido #${state.idPedido.takeLast(6).uppercase()} ha sido realizado con éxito."
                        )
                        cartViewModel.resetState()
                        findNavController().navigate(R.id.homeFragment)
                    }
                    is com.example.atresanosapp.presentation.client.cart.CartState.Error -> {
                        Toast.makeText(context, "Error: ${state.error}", Toast.LENGTH_LONG).show()
                        binding.btnConfirmOrder.isEnabled = true
                        binding.btnConfirmOrder.text = "Confirmar Pedido"
                    }
                    else -> {
                        binding.btnConfirmOrder.isEnabled = true
                        binding.btnConfirmOrder.text = "Confirmar Pedido"
                    }
                }
            }
        }

        binding.btnConfirmOrder.setOnClickListener {
            if (!dateSelected || !timeSelected) {
                Toast.makeText(context, "Por favor selecciona fecha y hora de entrega", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val target = googleMap?.cameraPosition?.target
            if (target != null) {
                val metodoPago = if (binding.rbTarjeta.isChecked) com.example.atresanosapp.data.model.MetodoPago.TARJETA else com.example.atresanosapp.data.model.MetodoPago.EFECTIVO
                val timestamp = Timestamp(selectedDate.time)
                cartViewModel.procesarCheckout(target.latitude, target.longitude, metodoPago, timestamp)
            } else {
                Toast.makeText(context, "El mapa no está listo aún", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseFirestore.getInstance().collection("usuarios").document(currentUser.uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists() && doc.contains("latitud") && doc.contains("longitud")) {
                        val lat = doc.getDouble("latitud") ?: 0.0
                        val lng = doc.getDouble("longitud") ?: 0.0
                        if (lat != 0.0 || lng != 0.0) {
                            val location = LatLng(lat, lng)
                            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 16f))
                            return@addOnSuccessListener
                        }
                    }
                    // Default fallback
                    val defaultLocation = LatLng(19.432608, -99.133209) // CDMX
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))
                }
        } else {
            val defaultLocation = LatLng(19.432608, -99.133209) // CDMX
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))
        }
    }

    private fun showDatePicker() {
        val current = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedDate.set(Calendar.YEAR, year)
                selectedDate.set(Calendar.MONTH, month)
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                dateSelected = true
                binding.btnSelectDate.text = "$dayOfMonth/${month + 1}/$year"
            },
            current.get(Calendar.YEAR),
            current.get(Calendar.MONTH),
            current.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        val current = Calendar.getInstance()
        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedDate.set(Calendar.MINUTE, minute)
                selectedDate.set(Calendar.SECOND, 0)
                timeSelected = true
                binding.btnSelectTime.text = String.format("%02d:%02d", hourOfDay, minute)
            },
            current.get(Calendar.HOUR_OF_DAY),
            current.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun searchAddress(addressText: String) {
        if (addressText.isBlank()) return

        val geocoder = android.location.Geocoder(requireContext())
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val addresses = geocoder.getFromLocationName(addressText, 1)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (!addresses.isNullOrEmpty()) {
                        val location = addresses[0]
                        val latLng = LatLng(location.latitude, location.longitude)
                        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                    } else {
                        Toast.makeText(context, "Dirección no encontrada", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: java.io.IOException) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(context, "Error al buscar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
