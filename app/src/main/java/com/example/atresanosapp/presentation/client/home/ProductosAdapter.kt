package com.example.atresanosapp.presentation.client.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.atresanosapp.data.model.Producto
import com.example.atresanosapp.databinding.ItemProductoBinding

class ProductosAdapter(
    private val onAddToCart: (Producto) -> Unit,
    private val onItemClick: ((Producto) -> Unit)? = null
) : RecyclerView.Adapter<ProductosAdapter.ProductoViewHolder>() {

    private var productos = listOf<Producto>()

    fun submitList(newList: List<Producto>) {
        productos = newList
        notifyDataSetChanged()
    }

    inner class ProductoViewHolder(private val binding: ItemProductoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(producto: Producto) {
            binding.tvNombre.text = producto.nombre
            binding.tvPrecio.text = "$${String.format("%.2f", producto.precioVenta)}"
            
            if (producto.stock > 0) {
                binding.tvStock.text = "En stock: ${producto.stock}"
                binding.tvStock.setTextColor(Color.parseColor("#4CAF50")) // primary
                binding.btnAddCart.isEnabled = true
                binding.btnAddCart.alpha = 1.0f
            } else {
                binding.tvStock.text = "Agotado"
                binding.tvStock.setTextColor(Color.RED)
                binding.btnAddCart.isEnabled = false
                binding.btnAddCart.alpha = 0.5f
            }

            binding.btnAddCart.setOnClickListener {
                onAddToCart(producto)
            }
            
            binding.root.setOnClickListener {
                onItemClick?.invoke(producto)
            }
            
            producto.urlsMultimedia.firstOrNull()?.let { url ->
                Glide.with(binding.ivProducto.context)
                    .load(url)
                    .into(binding.ivProducto)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val binding = ItemProductoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        holder.bind(productos[position])
    }

    override fun getItemCount(): Int = productos.size
}
