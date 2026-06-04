package com.example.atresanosapp.presentation.admin.routes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.atresanosapp.data.model.Pedido
import com.example.atresanosapp.databinding.ItemRouteOrderBinding

import java.text.SimpleDateFormat
import java.util.Locale
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RouteOrderAdapter(
    private val onSelectionChanged: (List<Pedido>) -> Unit,
    private val onDeliveredClick: (Pedido) -> Unit
) : RecyclerView.Adapter<RouteOrderAdapter.RouteViewHolder>() {

    private var pedidos = listOf<Pedido>()
    private val selectedPedidos = mutableSetOf<String>()
    private val db = FirebaseFirestore.getInstance()

    fun submitList(newList: List<Pedido>) {
        pedidos = newList
        notifyDataSetChanged()
    }

    fun getSelectedOrders(): List<Pedido> {
        return pedidos.filter { selectedPedidos.contains(it.id) }
    }

    inner class RouteViewHolder(private val binding: ItemRouteOrderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pedido: Pedido) {
            binding.tvRouteClient.text = "Cliente: Cargando..."
            if (pedido.nombreCliente.isNotBlank()) {
                binding.tvRouteClient.text = "Cliente: ${pedido.nombreCliente}"
            } else if (pedido.idUsuario.isNotBlank()) {
                db.collection("usuarios").document(pedido.idUsuario).get()
                    .addOnSuccessListener { doc ->
                        val nombre = doc.getString("nombre") ?: "Desconocido"
                        binding.tvRouteClient.text = "Cliente: $nombre"
                    }
            } else {
                binding.tvRouteClient.text = "Cliente: Desconocido"
            }

            binding.tvRouteId.text = "Pedido: #${pedido.id.takeLast(6).uppercase()}"
            binding.tvRouteAddress.text = "Cargando dirección..."
            
            val lat = pedido.latitudEntrega
            val lng = pedido.longitudEntrega
            
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val geocoder = android.location.Geocoder(binding.root.context, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    val addressText = if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        val subThoroughfare = address.subThoroughfare ?: ""
                        val thoroughfare = address.thoroughfare ?: ""
                        val locality = address.locality ?: ""
                        val featureName = address.featureName ?: ""
                        
                        // Try to construct a readable address
                        if (thoroughfare.isNotBlank()) {
                            "$thoroughfare $subThoroughfare, $locality".trim(',', ' ')
                        } else if (featureName.isNotBlank() && featureName != subThoroughfare) {
                            "$featureName, $locality".trim(',', ' ')
                        } else {
                            address.getAddressLine(0) ?: "Dirección desconocida"
                        }
                    } else {
                        "Lat: ${String.format("%.4f", lat)}, Lng: ${String.format("%.4f", lng)}"
                    }
                    
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        binding.tvRouteAddress.text = addressText
                    }
                } catch (e: Exception) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        binding.tvRouteAddress.text = "Lat: ${String.format("%.4f", lat)}, Lng: ${String.format("%.4f", lng)}"
                    }
                }
            }
            
            if (pedido.fechaHoraMaximaEntrega != null) {
                val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
                binding.tvRouteDate.text = "Entrega: ${sdf.format(pedido.fechaHoraMaximaEntrega.toDate())}"
            } else {
                binding.tvRouteDate.text = "Entrega: Sin fecha"
            }
            
            // Remove listener to prevent triggering while setting checked state programmatically
            binding.cbSelectRoute.setOnCheckedChangeListener(null)
            binding.cbSelectRoute.isChecked = selectedPedidos.contains(pedido.id)
            
            binding.cbSelectRoute.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedPedidos.add(pedido.id)
                } else {
                    selectedPedidos.remove(pedido.id)
                }
                onSelectionChanged(getSelectedOrders())
            }

            binding.root.setOnClickListener {
                binding.cbSelectRoute.isChecked = !binding.cbSelectRoute.isChecked
            }

            binding.btnMarkDelivered.setOnClickListener {
                onDeliveredClick(pedido)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val binding = ItemRouteOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RouteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        holder.bind(pedidos[position])
    }

    override fun getItemCount(): Int = pedidos.size
}
