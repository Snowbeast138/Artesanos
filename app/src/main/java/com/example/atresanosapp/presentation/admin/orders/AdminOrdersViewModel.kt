package com.example.atresanosapp.presentation.admin.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.atresanosapp.data.model.EstadoPedido
import com.example.atresanosapp.data.model.Pedido
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminOrdersViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _allPedidosActivos = MutableStateFlow<List<Pedido>>(emptyList())
    private val _allPedidosCompletados = MutableStateFlow<List<Pedido>>(emptyList())
    private val _allPedidosCancelados = MutableStateFlow<List<Pedido>>(emptyList())

    private val _searchQuery = MutableStateFlow("")

    private val _users = MutableStateFlow<Map<String, String>>(emptyMap())

    val pedidosActivos: StateFlow<List<Pedido>> = combine(_allPedidosActivos, _searchQuery, _users) { pedidos, query, usersMap ->
        if (query.isBlank()) pedidos else pedidos.filter {
            val queryNormalized = normalizeString(query)
            val realName = if (it.nombreCliente.isNotBlank()) it.nombreCliente else (usersMap[it.idUsuario] ?: "")
            val nameNormalized = normalizeString(realName)
            nameNormalized.contains(queryNormalized) || it.id.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val pedidosCompletados: StateFlow<List<Pedido>> = combine(_allPedidosCompletados, _searchQuery, _users) { pedidos, query, usersMap ->
        if (query.isBlank()) pedidos else pedidos.filter {
            val queryNormalized = normalizeString(query)
            val realName = if (it.nombreCliente.isNotBlank()) it.nombreCliente else (usersMap[it.idUsuario] ?: "")
            val nameNormalized = normalizeString(realName)
            nameNormalized.contains(queryNormalized) || it.id.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    
    val pedidosCancelados: StateFlow<List<Pedido>> = combine(_allPedidosCancelados, _searchQuery, _users) { pedidos, query, usersMap ->
        if (query.isBlank()) pedidos else pedidos.filter {
            val queryNormalized = normalizeString(query)
            val realName = if (it.nombreCliente.isNotBlank()) it.nombreCliente else (usersMap[it.idUsuario] ?: "")
            val nameNormalized = normalizeString(realName)
            nameNormalized.contains(queryNormalized) || it.id.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun normalizeString(input: String): String {
        val normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
        val ascii = normalized.replace(Regex("[\\p{InCombiningDiacriticalMarks}]"), "")
        return ascii.lowercase()
    }

    init {
        listenToPedidos()
        listenToUsuarios()
    }

    private fun listenToUsuarios() {
        db.collection("usuarios").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val usersMap = mutableMapOf<String, String>()
            for (doc in snapshot.documents) {
                val id = doc.id
                val nombre = doc.getString("nombre") ?: ""
                usersMap[id] = nombre
            }
            _users.value = usersMap
        }
    }

    private fun listenToPedidos() {
        db.collection("pedidos").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            
            val todos = snapshot.toObjects(Pedido::class.java)
            val activos = mutableListOf<Pedido>()
            val completados = mutableListOf<Pedido>()
            val cancelados = mutableListOf<Pedido>()

            for (pedido in todos) {
                if (pedido.estado == EstadoPedido.CANCELADO) {
                    cancelados.add(pedido)
                } else if (pedido.estado == EstadoPedido.ENTREGADO) {
                    completados.add(pedido)
                } else {
                    activos.add(pedido)
                }
            }
            
            _allPedidosActivos.value = activos
            _allPedidosCompletados.value = completados
            _allPedidosCancelados.value = cancelados
        }
    }

    fun aplicarDescuento(pedidoId: String, nuevoDescuentoFijo: Double) {
        viewModelScope.launch {
            try {
                val docRef = db.collection("pedidos").document(pedidoId)
                val doc = docRef.get().await()
                val pedido = doc.toObject(Pedido::class.java) ?: return@launch

                val nuevoCostoNeto = pedido.costoBruto - nuevoDescuentoFijo - (pedido.costoBruto * pedido.descuentoPorcentaje / 100)
                
                docRef.update(
                    "descuentoFijo", nuevoDescuentoFijo,
                    "costoNeto", nuevoCostoNeto
                ).await()
            } catch (e: Exception) {
                // Manejar error si es necesario
            }
        }
    }

    fun abonarDinero(pedidoId: String, cantidad: Double) {
        viewModelScope.launch {
            try {
                val docRef = db.collection("pedidos").document(pedidoId)
                val doc = docRef.get().await()
                val pedido = doc.toObject(Pedido::class.java) ?: return@launch

                val nuevoAbono = pedido.montoAbonado + cantidad
                docRef.update("montoAbonado", nuevoAbono).await()
            } catch (e: Exception) {
            }
        }
    }

    fun liquidarPedido(pedidoId: String) {
        viewModelScope.launch {
            try {
                val docRef = db.collection("pedidos").document(pedidoId)
                val doc = docRef.get().await()
                val pedido = doc.toObject(Pedido::class.java) ?: return@launch

                docRef.update("montoAbonado", pedido.costoNeto).await()
            } catch (e: Exception) {
            }
        }
    }
}
