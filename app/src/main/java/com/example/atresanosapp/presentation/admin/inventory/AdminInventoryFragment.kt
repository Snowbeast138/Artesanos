package com.example.atresanosapp.presentation.admin.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.atresanosapp.R
import com.example.atresanosapp.databinding.FragmentAdminInventoryBinding
import com.example.atresanosapp.presentation.client.home.ProductosAdapter
import kotlinx.coroutines.launch

class AdminInventoryFragment : Fragment() {
    private var _binding: FragmentAdminInventoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AdminInventoryViewModel
    private lateinit var adapter: ProductosAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this)[AdminInventoryViewModel::class.java]
        
        adapter = ProductosAdapter(
            onAddToCart = { 
                Toast.makeText(context, "Modo admin: Haz clic en la tarjeta para editar", Toast.LENGTH_SHORT).show()
            },
            onItemClick = { producto ->
                val bundle = Bundle().apply {
                    putString("productId", producto.id)
                    putString("nombre", producto.nombre)
                    putString("descripcion", producto.descripcion)
                    putDouble("precio", producto.precioVenta)
                    putDouble("costo", producto.costoProduccion)
                    putInt("stock", producto.stock)
                    putString("codigoBarras", producto.codigoBarras)
                    putString("imageUrl", producto.urlsMultimedia.firstOrNull())
                }
                findNavController().navigate(R.id.action_adminInventoryFragment_to_adminAddProductFragment, bundle)
            }
        )

        binding.rvAdminInventory.layoutManager = GridLayoutManager(context, 2)
        binding.rvAdminInventory.adapter = adapter

        binding.fabAddProduct.setOnClickListener {
            findNavController().navigate(R.id.action_adminInventoryFragment_to_adminAddProductFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is InventoryState.ProductsLoaded -> {
                        adapter.submitList(state.productos)
                    }
                    is InventoryState.Error -> {
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
