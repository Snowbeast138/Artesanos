package com.example.atresanosapp.presentation.client.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.atresanosapp.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch

import androidx.navigation.fragment.findNavController
import com.example.atresanosapp.R

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: ProductosAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        
        adapter = ProductosAdapter(
            onAddToCart = { producto ->
                val bundle = Bundle().apply {
                    putString("productId", producto.id)
                }
                findNavController().navigate(R.id.action_homeFragment_to_productDetailFragment, bundle)
            }
        )

        binding.rvProductos.layoutManager = GridLayoutManager(context, 2)
        binding.rvProductos.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is HomeState.Loading -> {
                        // TODO: Mostrar progress bar
                    }
                    is HomeState.Success -> {
                        adapter.submitList(state.productos)
                    }
                    is HomeState.Error -> {
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
