package com.example.atresanosapp.presentation.client.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.atresanosapp.data.model.Pedido
import com.example.atresanosapp.domain.repository.PedidosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val historial: List<Pedido>) : ProfileState()
    data class Error(val error: String) : ProfileState()
}

class ProfileViewModel(
    private val pedidosRepository: PedidosRepository
) : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState

    fun cargarHistorial(idUsuario: String) {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            try {
                pedidosRepository.obtenerPedidosUsuario(idUsuario).collect { pedidos ->
                    _profileState.value = ProfileState.Success(pedidos)
                }
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}
