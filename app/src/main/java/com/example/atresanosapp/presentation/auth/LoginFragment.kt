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
import com.example.atresanosapp.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { idToken ->
                authViewModel.loginConGoogle(idToken)
            }
        } catch (e: ApiException) {
            Toast.makeText(context, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ViewModel básico sin DI
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
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        observarEstado()

        binding.btnLogin.setOnClickListener {
            val emailOrPhone = binding.etEmailPhone.text.toString()
            val password = binding.etPassword.text.toString()
            // Lógica simple: Si tiene '@' es correo, si no, lo trataremos como teléfono en el futuro
            if (emailOrPhone.contains("@")) {
                authViewModel.loginConCorreo(emailOrPhone, password)
            } else {
                Toast.makeText(context, "Login con teléfono en desarrollo. Usa correo.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.tvForgotPassword.setOnClickListener {
            val emailInput = EditText(requireContext())
            emailInput.hint = "Correo Electrónico"
            AlertDialog.Builder(requireContext())
                .setTitle("Restablecer Contraseña")
                .setMessage("Ingresa tu correo para recibir un enlace de recuperación.")
                .setView(emailInput)
                .setPositiveButton("Enviar") { _, _ ->
                    val email = emailInput.text.toString().trim()
                    if (email.isNotEmpty()) {
                        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Enlace enviado a $email", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        binding.btnLoginGoogle.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun observarEstado() {
        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Loading -> {
                        binding.btnLogin.isEnabled = false
                        binding.btnLogin.text = "Cargando..."
                    }
                    is AuthState.Success -> {
                        binding.btnLogin.isEnabled = true
                        binding.btnLogin.text = "Iniciar Sesión"
                        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                    }
                    is AuthState.Error -> {
                        binding.btnLogin.isEnabled = true
                        binding.btnLogin.text = "Iniciar Sesión"
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
