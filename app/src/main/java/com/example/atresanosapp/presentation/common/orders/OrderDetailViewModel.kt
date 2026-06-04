package com.example.atresanosapp.presentation.common.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.atresanosapp.data.model.Pedido
import com.example.atresanosapp.data.model.ProductoPedido
import com.example.atresanosapp.data.model.Usuario
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class OrderDetailViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _pedido = MutableStateFlow<Pedido?>(null)
    val pedido: StateFlow<Pedido?> = _pedido

    private val _cliente = MutableStateFlow<Usuario?>(null)
    val cliente: StateFlow<Usuario?> = _cliente

    fun loadOrder(pedidoId: String) {
        db.collection("pedidos").document(pedidoId).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val p = snapshot.toObject(Pedido::class.java)
            _pedido.value = p
            
            p?.idUsuario?.let { uid ->
                loadClient(uid)
            }
        }
    }

    private fun loadClient(uid: String) {
        db.collection("usuarios").document(uid).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            _cliente.value = snapshot.toObject(Usuario::class.java)
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
            } catch (e: Exception) {}
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
            } catch (e: Exception) {}
        }
    }

    fun liquidarPedido(pedidoId: String) {
        viewModelScope.launch {
            try {
                val docRef = db.collection("pedidos").document(pedidoId)
                val doc = docRef.get().await()
                val pedido = doc.toObject(Pedido::class.java) ?: return@launch

                docRef.update("montoAbonado", pedido.costoNeto).await()
            } catch (e: Exception) {}
        }
    }

    fun setProductoEntregado(pedidoId: String, producto: ProductoPedido, cantidadEntregada: Int, currentList: List<ProductoPedido>) {
        viewModelScope.launch {
            try {
                val newList = currentList.map { 
                    if (it.id == producto.id && it.idProducto == producto.idProducto) {
                        it.copy(cantidadEntregada = cantidadEntregada)
                    } else {
                        it
                    }
                }
                db.collection("pedidos").document(pedidoId).update("productos", newList).await()
            } catch (e: Exception) {}
        }
    }

    fun cancelarPedido(pedidoId: String, productos: List<ProductoPedido>, onSuccess: () -> Unit) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val userDoc = db.collection("usuarios").document(uid).get().await()
                val currentUserName = userDoc.getString("nombre") ?: "Desconocido"
                
                val batch = db.batch()
                
                // Regresar los productos al inventario
                for (prod in productos) {
                    val productRef = db.collection("productos").document(prod.idProducto)
                    val currentSnapshot = productRef.get().await()
                    val currentStock = currentSnapshot.getLong("stock")?.toInt() ?: 0
                    batch.update(productRef, "stock", currentStock + prod.cantidad)
                }
                
                // Actualizar estado del pedido
                val pedidoRef = db.collection("pedidos").document(pedidoId)
                batch.update(
                    pedidoRef,
                    "estado", com.example.atresanosapp.data.model.EstadoPedido.CANCELADO.name,
                    "canceladoPor", currentUserName
                )
                
                batch.commit().await()
                
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                // Log o notificar error
            }
        }
    }
}
