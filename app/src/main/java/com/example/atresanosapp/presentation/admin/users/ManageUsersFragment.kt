package com.example.atresanosapp.presentation.admin.users

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.atresanosapp.data.model.RolUsuario
import com.example.atresanosapp.data.model.Usuario
import com.example.atresanosapp.databinding.FragmentManageUsersBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.text.Normalizer

class ManageUsersFragment : Fragment() {
    private var _binding: FragmentManageUsersBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private lateinit var userAdapter: UserAdapter
    private var allUsers = listOf<Usuario>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbarManageUsers.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        userAdapter = UserAdapter { usuario, action ->
            handleUserAction(usuario, action)
        }
        
        binding.rvUsers.layoutManager = LinearLayoutManager(context)
        binding.rvUsers.adapter = userAdapter

        loadUsers()

        binding.svUserSearch.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterUsers(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterUsers(newText)
                return true
            }
        })
    }

    private fun loadUsers() {
        db.collection("usuarios").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            allUsers = snapshot.toObjects(Usuario::class.java)
            filterUsers(binding.svUserSearch.query.toString())
        }
    }

    private fun filterUsers(query: String?) {
        if (query.isNullOrBlank()) {
            userAdapter.submitList(allUsers)
            return
        }
        val q = removeAccents(query.lowercase())
        val filtered = allUsers.filter {
            removeAccents(it.nombre.lowercase()).contains(q) ||
            it.email.lowercase().contains(q)
        }
        userAdapter.submitList(filtered)
    }

    private fun removeAccents(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }

    private fun handleUserAction(usuario: Usuario, action: String) {
        val updates = mutableMapOf<String, Any>()
        var message = ""
        
        when (action) {
            "ADMIN" -> {
                updates["rol"] = RolUsuario.ADMIN.name
                message = "Cambiado a ADMIN"
            }
            "DEV" -> {
                updates["rol"] = RolUsuario.DEV.name
                message = "Cambiado a DEV"
            }
            "CLIENTE" -> {
                updates["rol"] = RolUsuario.CLIENTE.name
                message = "Cambiado a CLIENTE"
            }
            "BAJA" -> {
                AlertDialog.Builder(requireContext())
                    .setTitle("Dar de Baja")
                    .setMessage("¿Estás seguro de que quieres dar de baja a ${usuario.nombre}? Ya no podrá acceder a la app.")
                    .setPositiveButton("Sí") { _, _ ->
                        db.collection("usuarios").document(usuario.id).update("activo", false)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Usuario dado de baja", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .setNegativeButton("No", null)
                    .show()
                return
            }
            "ALTA" -> {
                updates["activo"] = true
                message = "Usuario reactivado"
            }
        }

        if (updates.isNotEmpty()) {
            db.collection("usuarios").document(usuario.id).update(updates)
                .addOnSuccessListener {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error al actualizar", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
