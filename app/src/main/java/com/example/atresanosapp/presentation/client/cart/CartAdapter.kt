package com.example.atresanosapp.presentation.client.cart

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.atresanosapp.data.model.ProductoPedido
import com.example.atresanosapp.databinding.ItemCartBinding

class CartAdapter(
    private val onDeleteClick: (ProductoPedido) -> Unit,
    private val onQuantityChange: (ProductoPedido, Int) -> Unit
) : ListAdapter<ProductoPedido, CartAdapter.CartViewHolder>(CartDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CartViewHolder(private val binding: ItemCartBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ProductoPedido) {
            binding.tvCartItemName.text = item.nombre
            binding.tvCartItemPrice.text = "$${String.format("%.2f", item.precioUnitario)}"
            
            // Set current qty silently without triggering text watcher
            binding.etCartQty.setText(item.cantidad.toString())
            
            binding.btnDeleteItem.setOnClickListener {
                onDeleteClick(item)
            }
            
            binding.btnCartMinus.setOnClickListener {
                if (item.cantidad > 1) {
                    onQuantityChange(item, item.cantidad - 1)
                }
            }
            
            binding.btnCartPlus.setOnClickListener {
                onQuantityChange(item, item.cantidad + 1)
            }
            
            binding.etCartQty.setOnEditorActionListener { v, actionId, event ->
                val newQty = v.text.toString().toIntOrNull()
                if (newQty != null && newQty > 0 && newQty != item.cantidad) {
                    onQuantityChange(item, newQty)
                } else if (newQty != item.cantidad) {
                    binding.etCartQty.setText(item.cantidad.toString())
                }
                false
            }
            
            binding.etCartQty.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val newQty = binding.etCartQty.text.toString().toIntOrNull()
                    if (newQty != null && newQty > 0 && newQty != item.cantidad) {
                        onQuantityChange(item, newQty)
                    } else if (newQty != item.cantidad) {
                        binding.etCartQty.setText(item.cantidad.toString())
                    }
                }
            }
        }
    }

    class CartDiffCallback : DiffUtil.ItemCallback<ProductoPedido>() {
        override fun areItemsTheSame(oldItem: ProductoPedido, newItem: ProductoPedido): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ProductoPedido, newItem: ProductoPedido): Boolean {
            return oldItem == newItem
        }
    }
}
