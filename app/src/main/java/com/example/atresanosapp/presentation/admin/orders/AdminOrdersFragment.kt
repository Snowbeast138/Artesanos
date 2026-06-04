package com.example.atresanosapp.presentation.admin.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.atresanosapp.databinding.FragmentAdminOrdersBinding

class AdminOrdersFragment : Fragment() {
    private var _binding: FragmentAdminOrdersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminOrdersViewModel by viewModels()
    private lateinit var orderAdapter: com.example.atresanosapp.presentation.common.orders.OrderAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        orderAdapter = com.example.atresanosapp.presentation.common.orders.OrderAdapter { pedido ->
            val bundle = android.os.Bundle().apply {
                putString("pedidoId", pedido.id)
            }
            findNavController().navigate(com.example.atresanosapp.R.id.action_adminOrdersFragment_to_orderDetailFragment, bundle)
        }

        binding.rvAdminOrders.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        binding.rvAdminOrders.adapter = orderAdapter

        // Observar tabs
        binding.tabsOrders.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                updateList(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        binding.etSearchClient.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.updateSearchQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Observar datos del ViewModel
        lifecycleScope.launchWhenStarted {
            viewModel.pedidosActivos.collect {
                if (binding.tabsOrders.selectedTabPosition == 0) {
                    orderAdapter.submitList(it)
                }
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.pedidosCompletados.collect {
                if (binding.tabsOrders.selectedTabPosition == 1) {
                    orderAdapter.submitList(it)
                }
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.pedidosCancelados.collect {
                if (binding.tabsOrders.selectedTabPosition == 2) {
                    orderAdapter.submitList(it)
                }
            }
        }
    }

    private fun updateList(tabPosition: Int) {
        if (tabPosition == 0) {
            orderAdapter.submitList(viewModel.pedidosActivos.value)
        } else if (tabPosition == 1) {
            orderAdapter.submitList(viewModel.pedidosCompletados.value)
        } else {
            orderAdapter.submitList(viewModel.pedidosCancelados.value)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
