package com.example.atresanosapp.presentation.client.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.atresanosapp.data.model.ProductoPedido
import com.example.atresanosapp.data.model.Pedido
import com.example.atresanosapp.data.model.MetodoPago
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class CartState {
    object Idle : CartState()
    object Loading : CartState()
    object Processing : CartState()
    data class Success(val idPedido: String) : CartState()
    data class Error(val error: String) : CartState()
}

class CartViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _cartItems = MutableStateFlow<List<ProductoPedido>>(emptyList())
    val cartItems: StateFlow<List<ProductoPedido>> = _cartItems

    private val _total = MutableStateFlow(0.0)
    val total: StateFlow<Double> = _total

    private val _cartState = MutableStateFlow<CartState>(CartState.Idle)
    val cartState: StateFlow<CartState> = _cartState

    init {
        listenToCart()
    }

    private fun listenToCart() {
        val uid = auth.currentUser?.uid ?: return
        _cartState.value = CartState.Loading

        db.collection("usuarios").document(uid).collection("carrito")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _cartState.value = CartState.Error(error.message ?: "Error al cargar carrito")
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val items = snapshot.map { doc ->
                        doc.toObject(ProductoPedido::class.java).copy(id = doc.id)
                    }
                    _cartItems.value = items
                    recalcularTotal(items)
                    _cartState.value = CartState.Idle
                }
            }
    }

    private fun recalcularTotal(items: List<ProductoPedido>) {
        _total.value = items.sumOf { it.precioUnitario * it.cantidad }
    }

    fun agregarAlCarrito(producto: ProductoPedido) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val cartRef = db.collection("usuarios").document(uid).collection("carrito")
                
                // Buscar si ya existe
                val existingDocs = cartRef.whereEqualTo("idProducto", producto.idProducto).get().await()
                if (!existingDocs.isEmpty) {
                    val doc = existingDocs.documents[0]
                    val currentCantidad = doc.getLong("cantidad") ?: 0L
                    cartRef.document(doc.id).update("cantidad", currentCantidad + producto.cantidad).await()
                } else {
                    cartRef.add(producto).await()
                }
            } catch (e: Exception) {
                _cartState.value = CartState.Error(e.message ?: "Error al añadir al carrito")
            }
        }
    }

    fun eliminarDelCarrito(idDocumento: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("usuarios").document(uid).collection("carrito").document(idDocumento).delete().await()
            } catch (e: Exception) {
                _cartState.value = CartState.Error(e.message ?: "Error al eliminar")
            }
        }
    }

    fun actualizarCantidad(idDocumento: String, idProducto: String, nuevaCantidad: Int) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                if (nuevaCantidad > 0) {
                    // Check stock in real-time
                    val prodDoc = db.collection("productos").document(idProducto).get().await()
                    val currentStock = prodDoc.getLong("stock")?.toInt() ?: 0

                    if (nuevaCantidad <= currentStock) {
                        db.collection("usuarios").document(uid).collection("carrito").document(idDocumento)
                            .update("cantidad", nuevaCantidad).await()
                    } else {
                        // Notify error and revert to max possible or current if max is 0
                        _cartState.value = CartState.Error("Stock insuficiente. Solo quedan $currentStock disponibles.")
                        val validQty = if (currentStock > 0) currentStock else 1
                        db.collection("usuarios").document(uid).collection("carrito").document(idDocumento)
                            .update("cantidad", validQty).await()
                    }
                } else {
                    eliminarDelCarrito(idDocumento)
                }
            } catch (e: Exception) {
                _cartState.value = CartState.Error(e.message ?: "Error al actualizar cantidad")
            }
        }
    }

    fun procesarCheckout(latitud: Double, longitud: Double, metodoPago: MetodoPago, fechaMaxima: com.google.firebase.Timestamp) {
        val uid = auth.currentUser?.uid ?: return
        val currentItems = _cartItems.value
        if (currentItems.isEmpty()) return

        viewModelScope.launch {
            _cartState.value = CartState.Processing
            try {
                val batch = db.batch()
                val pedidosRef = db.collection("pedidos").document()

                // Check stock and deduct
                for (item in currentItems) {
                    val productRef = db.collection("productos").document(item.idProducto)
                    val productSnapshot = productRef.get().await() // Warning: Not purely transactional this way, but simpler
                    val currentStock = productSnapshot.getLong("stock")?.toInt() ?: 0

                    if (currentStock < item.cantidad) {
                        _cartState.value = CartState.Error("No hay suficiente stock para: ${item.nombre}")
                        return@launch
                    }
                    batch.update(productRef, "stock", currentStock - item.cantidad)
                }

                // Fetch User Name for denormalization
                val userSnapshot = db.collection("usuarios").document(uid).get().await()
                val nombreCliente = userSnapshot.getString("nombre") ?: "Cliente Desconocido"

                // Create Pedido
                val nuevoPedido = Pedido(
                    id = pedidosRef.id,
                    idUsuario = uid,
                    nombreCliente = nombreCliente,
                    productos = currentItems,
                    costoBruto = _total.value,
                    costoNeto = _total.value, // Sin descuento al inicio
                    montoAbonado = 0.0, // Inicia sin pagos
                    latitudEntrega = latitud,
                    longitudEntrega = longitud,
                    metodoPago = metodoPago,
                    fechaHoraMaximaEntrega = fechaMaxima
                )
                batch.set(pedidosRef, nuevoPedido)

                // Empty cart
                val cartCollection = db.collection("usuarios").document(uid).collection("carrito").get().await()
                for (doc in cartCollection.documents) {
                    batch.delete(doc.reference)
                }

                batch.commit().await()
                _cartState.value = CartState.Success(pedidosRef.id)

            } catch (e: Exception) {
                _cartState.value = CartState.Error(e.message ?: "Error al procesar pago")
            }
        }
    }
    
    fun resetState() {
        _cartState.value = CartState.Idle
    }
}
