package com.example.atresanosapp.presentation.client.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.atresanosapp.R
import com.example.atresanosapp.data.repository.AuthRepositoryImpl
import com.example.atresanosapp.databinding.FragmentProfileBinding
import com.example.atresanosapp.presentation.auth.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // Usamos el AuthViewModel para el logout
    private val authViewModel: AuthViewModel by lazy {
        val repo = AuthRepositoryImpl(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance())
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(repo) as T
            }
        })[AuthViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser

        // Cargar datos actuales de Firestore
        currentUser?.let { user ->
            FirebaseFirestore.getInstance().collection("usuarios").document(user.uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        binding.tvUserName.text = doc.getString("nombre")?.takeIf { it.isNotBlank() } ?: "Usuario sin nombre"
                        binding.tvUserPhone.text = doc.getString("telefono")?.takeIf { it.isNotBlank() } ?: "Sin teléfono"
                        
                        val rolStr = doc.getString("rol") ?: ""
                        if (rolStr == com.example.atresanosapp.data.model.RolUsuario.ADMIN.name || rolStr == com.example.atresanosapp.data.model.RolUsuario.DEV.name) {
                            binding.btnMyOrders.text = "Administrar Usuarios"
                            binding.btnMyOrders.setOnClickListener {
                                findNavController().navigate(R.id.action_profileFragment_to_manageUsersFragment)
                            }
                        } else {
                            binding.btnMyOrders.text = "Mis Pedidos (Activos y Entregados)"
                            binding.btnMyOrders.setOnClickListener {
                                findNavController().navigate(R.id.action_profileFragment_to_clientOrdersFragment)
                            }
                        }
                    } else {
                        binding.tvUserName.text = user.email ?: "Usuario"
                        binding.tvUserPhone.text = "Sin teléfono"
                    }
                }
        }

        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }

        // Historial de compras
        val orderAdapter = com.example.atresanosapp.presentation.common.orders.OrderAdapter { pedido ->
            val bundle = Bundle().apply {
                putString("pedidoId", pedido.id)
            }
            findNavController().navigate(R.id.action_profileFragment_to_orderDetailFragment, bundle)
        }
        binding.rvHistory.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        binding.rvHistory.adapter = orderAdapter

        // Fetch user's orders
        currentUser?.uid?.let { uid ->
            FirebaseFirestore.getInstance().collection("pedidos")
                .whereEqualTo("idUsuario", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        val pedidos = snapshot.toObjects(com.example.atresanosapp.data.model.Pedido::class.java)
                            .sortedByDescending { it.fechaCreacion }
                        orderAdapter.submitList(pedidos)
                    }
                }
        }

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            findNavController().navigate(R.id.loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
