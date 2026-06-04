package com.example.atresanosapp.presentation.client.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.atresanosapp.data.model.Producto
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class HomeState {
    object Loading : HomeState()
    data class Success(val productos: List<Producto>) : HomeState()
    data class Error(val message: String) : HomeState()
}

class HomeViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    
    private val _state = MutableStateFlow<HomeState>(HomeState.Loading)
    val state: StateFlow<HomeState> = _state

    init {
        fetchProductos()
    }

    fun fetchProductos() {
        viewModelScope.launch {
            _state.value = HomeState.Loading
            try {
                // Escuchar cambios en tiempo real
                db.collection("productos").addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        _state.value = HomeState.Error(error.message ?: "Error desconocido")
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        val productos = snapshot.toObjects(Producto::class.java)
                        _state.value = HomeState.Success(productos)
                    }
                }
            } catch (e: Exception) {
                _state.value = HomeState.Error(e.message ?: "Error al cargar productos")
            }
        }
    }
}
