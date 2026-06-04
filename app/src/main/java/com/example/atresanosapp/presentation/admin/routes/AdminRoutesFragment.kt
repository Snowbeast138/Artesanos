package com.example.atresanosapp.presentation.admin.routes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.atresanosapp.data.model.EstadoPedido
import com.example.atresanosapp.databinding.FragmentAdminRoutesBinding
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collectLatest

class AdminRoutesFragment : Fragment() {
    private var _binding: FragmentAdminRoutesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminRoutesViewModel by viewModels()
    private lateinit var adapter: RouteOrderAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminRoutesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = RouteOrderAdapter(
            onSelectionChanged = { selected ->
                binding.fabGenerateRoute.text = "Generar Ruta (${selected.size})"
            },
            onDeliveredClick = { pedido ->
                val itemsCompletos = pedido.productos.all { it.cantidadEntregada >= it.cantidad }
                val deuda = pedido.costoNeto - pedido.montoAbonado
                
                if (deuda > 0.01 || !itemsCompletos) {
                    Toast.makeText(context, "No se puede entregar: hay deuda pendiente o faltan productos por distribuir.", Toast.LENGTH_LONG).show()
                } else {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Marcar como Entregado")
                        .setMessage("¿Estás seguro de que deseas marcar el pedido #${pedido.id.takeLast(6).uppercase()} como ENTREGADO?")
                        .setPositiveButton("Sí") { _, _ ->
                            db.collection("pedidos").document(pedido.id)
                                .update("estado", EstadoPedido.ENTREGADO.name)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Pedido entregado", Toast.LENGTH_SHORT).show()
                                    
                                    db.collection("usuarios").document(pedido.idUsuario).get()
                                        .addOnSuccessListener { doc ->
                                            val cliente = doc.toObject(com.example.atresanosapp.data.model.Usuario::class.java)
                                            if (cliente != null) {
                                                val options = mutableListOf<String>()
                                                if (cliente.telefono.isNotEmpty()) options.add("WhatsApp")
                                                if (cliente.email.isNotEmpty()) options.add("Correo Electrónico")
                                                
                                                if (options.isNotEmpty()) {
                                                    AlertDialog.Builder(requireContext())
                                                        .setTitle("Enviar Factura/Nota a través de:")
                                                        .setItems(options.toTypedArray()) { _, which ->
                                                            val method = if (options[which] == "WhatsApp") "WhatsApp" else "Email"
                                                            com.example.atresanosapp.utils.PdfHelper.generateAndShareInvoice(
                                                                requireContext(),
                                                                pedido,
                                                                cliente,
                                                                method
                                                            )
                                                        }
                                                        .show()
                                                }
                                            }
                                        }
                                }
                        }
                        .setNegativeButton("No", null)
                        .show()
                }
            }
        )
        
        binding.rvRouteOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRouteOrders.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.pedidosPendientes.collectLatest { pedidos ->
                adapter.submitList(pedidos)
            }
        }

        binding.fabGenerateRoute.setOnClickListener {
            val selected = adapter.getSelectedOrders()
            if (selected.isEmpty()) {
                Toast.makeText(context, "Selecciona al menos un pedido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Construir la URI para Google Maps
            // La API de Google Maps Directions App:
            // https://www.google.com/maps/dir/?api=1&destination=lat,lng&waypoints=lat1,lng1|lat2,lng2&travelmode=driving
            
            val validOrders = selected.filter { it.latitudEntrega != 0.0 || it.longitudEntrega != 0.0 }
            if (validOrders.isEmpty()) {
                Toast.makeText(context, "Los pedidos seleccionados no tienen una dirección válida guardada.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val destination = validOrders.last()
            val destinationStr = "${destination.latitudEntrega},${destination.longitudEntrega}"
            
            var waypointsStr = ""
            if (validOrders.size > 1) {
                val waypoints = validOrders.dropLast(1)
                waypointsStr = "&waypoints=" + waypoints.joinToString("|") { "${it.latitudEntrega},${it.longitudEntrega}" }
            }

            val uriStr = "https://www.google.com/maps/dir/?api=1&destination=$destinationStr$waypointsStr&travelmode=driving"
            
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr))
            intent.setPackage("com.google.android.apps.maps")
            try {
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback si no tiene google maps
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr))
                startActivity(browserIntent)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
