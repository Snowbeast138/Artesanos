package com.example.atresanosapp.presentation.client.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.atresanosapp.R
import com.example.atresanosapp.databinding.FragmentEditProfileBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileFragment : Fragment() {
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private var latitudSeleccionada: Double = 0.0
    private var longitudSeleccionada: Double = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser

        // Cargar datos existentes
        currentUser?.let { user ->
            FirebaseFirestore.getInstance().collection("usuarios").document(user.uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        binding.etEditName.setText(doc.getString("nombre") ?: "")
                        binding.etEditPhone.setText(doc.getString("telefono") ?: "")
                        latitudSeleccionada = doc.getDouble("latitud") ?: 0.0
                        longitudSeleccionada = doc.getDouble("longitud") ?: 0.0
                        updateLocationText()
                    }
                }
        }

        parentFragmentManager.setFragmentResultListener("locationResult", viewLifecycleOwner) { _, bundle ->
            latitudSeleccionada = bundle.getDouble("latitud")
            longitudSeleccionada = bundle.getDouble("longitud")
            updateLocationText()
            
            // Auto-guardado de ubicación
            currentUser?.let { user ->
                val updates = mapOf(
                    "latitud" to latitudSeleccionada,
                    "longitud" to longitudSeleccionada
                )
                FirebaseFirestore.getInstance().collection("usuarios").document(user.uid)
                    .update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Ubicación actualizada en la nube", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        binding.btnChangeLocation.setOnClickListener {
            val bundle = Bundle().apply {
                putDouble("latitud", latitudSeleccionada)
                putDouble("longitud", longitudSeleccionada)
            }
            findNavController().navigate(R.id.action_editProfileFragment_to_mapLocationFragment, bundle)
        }

        binding.btnSaveEdit.setOnClickListener {
            val newName = binding.etEditName.text.toString().trim()
            val newPhone = binding.etEditPhone.text.toString().trim()

            if (newName.isNotEmpty() && currentUser != null) {
                val updates = mapOf(
                    "nombre" to newName,
                    "telefono" to newPhone,
                    "latitud" to latitudSeleccionada,
                    "longitud" to longitudSeleccionada
                )
                FirebaseFirestore.getInstance().collection("usuarios").document(currentUser.uid)
                    .update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Perfil guardado", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Error al guardar", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(context, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnResetPwdEdit.setOnClickListener {
            currentUser?.email?.let { email ->
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Correo enviado a $email", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            } ?: run {
                Toast.makeText(context, "No hay correo registrado", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnDeleteAccount.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Eliminar Cuenta")
                .setMessage("¿Estás seguro de que deseas eliminar tu cuenta? Esta acción te desconectará y tu perfil ya no será accesible.")
                .setPositiveButton("Sí, eliminar") { _, _ ->
                    eliminarCuentaSoft(currentUser?.uid)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun updateLocationText() {
        if (latitudSeleccionada != 0.0 && longitudSeleccionada != 0.0) {
            binding.tvLocationStatus.text = "Ubicación: $latitudSeleccionada, $longitudSeleccionada"
        } else {
            binding.tvLocationStatus.text = "Ubicación: No establecida"
        }
    }

    private fun eliminarCuentaSoft(uid: String?) {
        if (uid == null) return

        // Soft delete: cambiar activo a false
        FirebaseFirestore.getInstance().collection("usuarios").document(uid)
            .update("activo", false)
            .addOnSuccessListener {
                Toast.makeText(context, "Cuenta eliminada correctamente", Toast.LENGTH_SHORT).show()
                FirebaseAuth.getInstance().signOut()
                // Redirigir al Login y limpiar backstack
                findNavController().navigate(R.id.loginFragment)
            }
            .addOnFailureListener {
                Toast.makeText(context, "No se pudo eliminar la cuenta", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
