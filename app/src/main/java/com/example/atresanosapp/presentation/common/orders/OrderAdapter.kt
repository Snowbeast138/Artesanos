package com.example.atresanosapp.presentation.common.orders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.atresanosapp.R
import com.example.atresanosapp.data.model.Pedido
import com.example.atresanosapp.databinding.ItemOrderBinding

class OrderAdapter(
    private val onItemClick: (Pedido) -> Unit
) : ListAdapter<Pedido, OrderAdapter.OrderViewHolder>(OrderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class OrderViewHolder(
        private val binding: ItemOrderBinding,
        private val onItemClick: (Pedido) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(pedido: Pedido) {
            val deuda = pedido.costoNeto - pedido.montoAbonado

            binding.tvOrderId.text = "Pedido: #${pedido.id.takeLast(6).uppercase()}"
            
            if (pedido.nombreCliente.isNotEmpty()) {
                binding.tvOrderClient.text = "Cliente: ${pedido.nombreCliente}"
            } else {
                binding.tvOrderClient.text = "Cargando cliente..."
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(pedido.idUsuario)
                    .get()
                    .addOnSuccessListener { doc ->
                        val nombre = doc.getString("nombre") ?: "Cliente Desconocido"
                        binding.tvOrderClient.text = "Cliente: $nombre"
                    }
                    .addOnFailureListener {
                        binding.tvOrderClient.text = "Cliente: Desconocido"
                    }
            }

            val dateStr = pedido.fechaHoraMaximaEntrega?.toDate()?.let { date ->
                java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a", java.util.Locale.getDefault()).format(date)
            } ?: "No especificada"
            binding.tvOrderDate.text = "Entrega: $dateStr"

            binding.tvOrderDeliveryStatus.text = "Estado: ${pedido.estado.name}"
            binding.tvOrderTotal.text = "Total: $${String.format("%.2f", pedido.costoNeto)}"
            binding.tvOrderPaid.text = "Abonado: $${String.format("%.2f", pedido.montoAbonado)}"
            binding.tvOrderDebt.text = "Deuda: $${String.format("%.2f", deuda)}"

            // Status Badge
            when {
                pedido.montoAbonado >= pedido.costoNeto -> {
                    binding.tvPaymentStatus.text = "Pagado"
                    binding.cvPaymentStatus.setCardBackgroundColor(
                        ContextCompat.getColor(binding.root.context, android.R.color.holo_green_dark)
                    )
                }
                pedido.montoAbonado > 0.0 -> {
                    binding.tvPaymentStatus.text = "Pendiente"
                    binding.cvPaymentStatus.setCardBackgroundColor(
                        ContextCompat.getColor(binding.root.context, android.R.color.holo_orange_dark)
                    )
                }
                else -> {
                    binding.tvPaymentStatus.text = "Sin Pago"
                    binding.cvPaymentStatus.setCardBackgroundColor(
                        ContextCompat.getColor(binding.root.context, android.R.color.holo_red_dark)
                    )
                }
            }

            binding.root.setOnClickListener {
                onItemClick(pedido)
            }
        }
    }

    class OrderDiffCallback : DiffUtil.ItemCallback<Pedido>() {
        override fun areItemsTheSame(oldItem: Pedido, newItem: Pedido) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Pedido, newItem: Pedido) = oldItem == newItem
    }
}
