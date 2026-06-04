package com.example.atresanosapp.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.atresanosapp.data.model.Usuario
import com.example.atresanosapp.domain.repository.AuthRepository
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val usuario: Usuario) : AuthState()
    data class Error(val error: String) : AuthState()
}

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    init {
        verificarSesion()
    }

    private fun verificarSesion() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.obtenerUsuarioActual().fold(
                onSuccess = { _authState.value = AuthState.Success(it!!) },
                onFailure = { _authState.value = AuthState.Idle } // Not logged in
            )
        }
    }

    fun loginConCorreo(email: String, contrasenia: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.iniciarSesionConCorreo(email, contrasenia).fold(
                onSuccess = { _authState.value = AuthState.Success(it) },
                onFailure = { _authState.value = AuthState.Error(it.message ?: "Error desconocido") }
            )
        }
    }

    fun registrarConCorreo(email: String, contrasenia: String, nombre: String, telefono: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.registrarConCorreo(email, contrasenia, nombre, telefono).fold(
                onSuccess = { _authState.value = AuthState.Success(it) },
                onFailure = { _authState.value = AuthState.Error(it.message ?: "Error desconocido") }
            )
        }
    }

    fun loginConGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.iniciarSesionConCredencialGoogle(idToken).fold(
                onSuccess = { _authState.value = AuthState.Success(it) },
                onFailure = { _authState.value = AuthState.Error(it.message ?: "Error desconocido") }
            )
        }
    }
}
