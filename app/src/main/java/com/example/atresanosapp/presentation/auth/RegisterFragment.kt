package com.example.atresanosapp.presentation.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.atresanosapp.R
import com.example.atresanosapp.data.repository.AuthRepositoryImpl
import com.example.atresanosapp.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

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
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observarEstado()

        binding.btnRegistrar.setOnClickListener {
            val nombre = binding.etNombre.text.toString()
            val telefono = binding.etTelefono.text.toString()
            val email = binding.etEmailReg.text.toString()
            val password = binding.etPasswordReg.text.toString()

            if (nombre.isNotBlank() && email.isNotBlank() && password.length >= 6) {
                authViewModel.registrarConCorreo(email, password, nombre, telefono)
            } else {
                Toast.makeText(context, "Por favor llena todos los campos correctamente", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observarEstado() {
        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Loading -> {
                        binding.btnRegistrar.isEnabled = false
                        binding.btnRegistrar.text = "Registrando..."
                    }
                    is AuthState.Success -> {
                        binding.btnRegistrar.isEnabled = true
                        findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
                    }
                    is AuthState.Error -> {
                        binding.btnRegistrar.isEnabled = true
                        binding.btnRegistrar.text = "Registrarme"
                        Toast.makeText(context, state.error, Toast.LENGTH_LONG).show()
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
