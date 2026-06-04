package com.example.atresanosapp.presentation.client.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.atresanosapp.databinding.FragmentClientOrdersBinding
import com.example.atresanosapp.presentation.common.orders.OrderAdapter

class ClientOrdersFragment : Fragment() {
    private var _binding: FragmentClientOrdersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ClientOrdersViewModel by viewModels()
    private lateinit var orderAdapter: OrderAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClientOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbarClientOrders.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        orderAdapter = OrderAdapter { pedido ->
            val bundle = Bundle().apply {
                putString("pedidoId", pedido.id)
            }
            findNavController().navigate(com.example.atresanosapp.R.id.action_clientOrdersFragment_to_orderDetailFragment, bundle)
        }

        binding.rvClientOrders.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        binding.rvClientOrders.adapter = orderAdapter

        binding.tabsClientOrders.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                updateList(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        lifecycleScope.launchWhenStarted {
            viewModel.pedidosActivos.collect {
                if (binding.tabsClientOrders.selectedTabPosition == 0) {
                    orderAdapter.submitList(it)
                }
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.pedidosCompletados.collect {
                if (binding.tabsClientOrders.selectedTabPosition == 1) {
                    orderAdapter.submitList(it)
                }
            }
        }
    }

    private fun updateList(tabPosition: Int) {
        if (tabPosition == 0) {
            orderAdapter.submitList(viewModel.pedidosActivos.value)
        } else {
            orderAdapter.submitList(viewModel.pedidosCompletados.value)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
