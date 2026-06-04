package com.example.atresanosapp.presentation.admin.routes

import androidx.lifecycle.ViewModel
import com.example.atresanosapp.data.model.EstadoPedido
import com.example.atresanosapp.data.model.Pedido
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AdminRoutesViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _pedidosPendientes = MutableStateFlow<List<Pedido>>(emptyList())
    val pedidosPendientes: StateFlow<List<Pedido>> = _pedidosPendientes

    init {
        listenToPedidosPendientes()
    }

    private fun listenToPedidosPendientes() {
        db.collection("pedidos").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            
            val todos = snapshot.toObjects(Pedido::class.java)
            val pendientes = mutableListOf<Pedido>()

            for (pedido in todos) {
                if (pedido.estado != EstadoPedido.ENTREGADO) {
                    pendientes.add(pedido)
                }
            }
            
            _pedidosPendientes.value = pendientes
        }
    }
}
