package com.example.atresanosapp.presentation.client.cart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.atresanosapp.R
import com.example.atresanosapp.databinding.FragmentCartBinding

class CartFragment : Fragment() {
    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    private val cartViewModel: CartViewModel by activityViewModels()
    private lateinit var cartAdapter: CartAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cartAdapter = CartAdapter(
            onDeleteClick = { item ->
                cartViewModel.eliminarDelCarrito(item.id)
            },
            onQuantityChange = { item, newQty ->
                cartViewModel.actualizarCantidad(item.id, item.idProducto, newQty)
            }
        )
        binding.rvCart.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        binding.rvCart.adapter = cartAdapter

        lifecycleScope.launchWhenStarted {
            cartViewModel.cartItems.collect { items ->
                cartAdapter.submitList(items)
                binding.btnCheckout.isEnabled = items.isNotEmpty()
            }
        }

        lifecycleScope.launchWhenStarted {
            cartViewModel.total.collect { total ->
                binding.tvTotalAmount.text = "$${String.format("%.2f", total)}"
            }
        }

        lifecycleScope.launchWhenStarted {
            cartViewModel.cartState.collect { state ->
                when (state) {
                    is CartState.Error -> {
                        android.widget.Toast.makeText(context, state.error, android.widget.Toast.LENGTH_LONG).show()
                        cartViewModel.resetState()
                    }
                    else -> {}
                }
            }
        }

        binding.btnCheckout.setOnClickListener {
            findNavController().navigate(com.example.atresanosapp.R.id.action_cartFragment_to_checkoutFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
