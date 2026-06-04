package com.example.atresanosapp.presentation.admin.stats

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.atresanosapp.databinding.ItemBuyerBinding

data class BuyerStat(val nombre: String, val cantidad: Int)

class BuyerAdapter : RecyclerView.Adapter<BuyerAdapter.BuyerViewHolder>() {

    private var items = listOf<BuyerStat>()

    fun submitList(newList: List<BuyerStat>) {
        items = newList.sortedByDescending { it.cantidad }
        notifyDataSetChanged()
    }

    inner class BuyerViewHolder(private val binding: ItemBuyerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(stat: BuyerStat) {
            binding.tvBuyerName.text = "Cliente: ${stat.nombre}"
            binding.tvBuyerQuantity.text = "Compró: ${stat.cantidad} uds"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuyerViewHolder {
        val binding = ItemBuyerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BuyerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BuyerViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
