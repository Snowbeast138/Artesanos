package com.example.atresanosapp.presentation.client.orders

import androidx.lifecycle.ViewModel
import com.example.atresanosapp.data.model.EstadoPedido
import com.example.atresanosapp.data.model.Pedido
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ClientOrdersViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _pedidosActivos = MutableStateFlow<List<Pedido>>(emptyList())
    val pedidosActivos: StateFlow<List<Pedido>> = _pedidosActivos

    private val _pedidosCompletados = MutableStateFlow<List<Pedido>>(emptyList())
    val pedidosCompletados: StateFlow<List<Pedido>> = _pedidosCompletados

    init {
        listenToMyOrders()
    }

    private fun listenToMyOrders() {
        val uid = auth.currentUser?.uid ?: return
        
        db.collection("pedidos")
            .whereEqualTo("idUsuario", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                
                val todos = snapshot.toObjects(Pedido::class.java)
                val activos = mutableListOf<Pedido>()
                val completados = mutableListOf<Pedido>()

                for (pedido in todos) {
                    val pagado = pedido.montoAbonado >= pedido.costoNeto
                    if (pedido.estado == EstadoPedido.ENTREGADO && pagado) {
                        completados.add(pedido)
                    } else if (pedido.estado == EstadoPedido.CANCELADO) {
                        completados.add(pedido)
                    } else {
                        activos.add(pedido)
                    }
                }
                
                _pedidosActivos.value = activos.sortedByDescending { it.fechaCreacion }
                _pedidosCompletados.value = completados.sortedByDescending { it.fechaCreacion }
            }
    }
}
