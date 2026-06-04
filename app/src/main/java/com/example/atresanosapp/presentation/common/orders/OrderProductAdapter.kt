package com.example.atresanosapp.presentation.common.orders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.atresanosapp.data.model.ProductoPedido
import com.example.atresanosapp.databinding.ItemOrderProductBinding

class OrderProductAdapter(
    private var products: List<ProductoPedido> = emptyList(),
    private val isAdmin: Boolean = false,
    private val onItemClick: ((ProductoPedido, Int) -> Unit)? = null
) : RecyclerView.Adapter<OrderProductAdapter.ViewHolder>() {

    fun submitList(list: List<ProductoPedido>) {
        products = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOrderProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(products[position], position)
    }

    override fun getItemCount() = products.size

    inner class ViewHolder(private val binding: ItemOrderProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: ProductoPedido, position: Int) {
            binding.tvOpQty.text = "${product.cantidadEntregada}/${product.cantidad}x"
            binding.tvOpName.text = product.nombre
            binding.tvOpPrice.text = "$${String.format("%.2f", product.precioUnitario * product.cantidad)}"
            
            if (product.cantidadEntregada >= product.cantidad) {
                binding.ivOpStatus.setImageResource(com.example.atresanosapp.R.drawable.ic_check)
                binding.ivOpStatus.setColorFilter(androidx.core.content.ContextCompat.getColor(binding.root.context, android.R.color.holo_green_dark))
            } else if (product.cantidadEntregada > 0) {
                binding.ivOpStatus.setImageResource(com.example.atresanosapp.R.drawable.ic_minus)
                binding.ivOpStatus.setColorFilter(androidx.core.content.ContextCompat.getColor(binding.root.context, android.R.color.holo_orange_dark))
            } else {
                binding.ivOpStatus.setImageResource(android.R.drawable.ic_delete)
                binding.ivOpStatus.setColorFilter(androidx.core.content.ContextCompat.getColor(binding.root.context, android.R.color.holo_red_dark))
            }

            if (isAdmin) {
                binding.root.setOnClickListener {
                    onItemClick?.invoke(product, position)
                }
            } else {
                binding.root.setOnClickListener(null)
            }
        }
    }
}
