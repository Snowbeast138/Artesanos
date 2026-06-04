package com.example.atresanosapp.presentation.admin.inventory

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.atresanosapp.data.model.Producto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

sealed class InventoryState {
    object Idle : InventoryState()
    object Loading : InventoryState()
    object Success : InventoryState()
    data class Error(val message: String) : InventoryState()
    data class ProductsLoaded(val productos: List<Producto>) : InventoryState()
}

class AdminInventoryViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance().reference
    
    private val _state = MutableStateFlow<InventoryState>(InventoryState.Idle)
    val state: StateFlow<InventoryState> = _state

    init {
        fetchProductos()
    }

    fun fetchProductos() {
        viewModelScope.launch {
            _state.value = InventoryState.Loading
            try {
                db.collection("productos").addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        _state.value = InventoryState.Error(error.message ?: "Error desconocido")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val productos = snapshot.map { doc ->
                            doc.toObject(Producto::class.java).copy(id = doc.id)
                        }
                        _state.value = InventoryState.ProductsLoaded(productos)
                    }
                }
            } catch (e: Exception) {
                _state.value = InventoryState.Error(e.message ?: "Error al cargar productos")
            }
        }
    }

    fun addOrUpdateProduct(
        productId: String?,
        nombre: String, 
        descripcion: String, 
        precio: Double, 
        costo: Double, 
        stock: Int,
        codigoBarras: String,
        imageUri: Uri?,
        existingImageUrl: String?
    ) {
        viewModelScope.launch {
            _state.value = InventoryState.Loading
            try {
                var imageUrl = existingImageUrl

                // If a new image was selected, upload it
                if (imageUri != null) {
                    val fileName = "productos/${UUID.randomUUID()}.jpg"
                    val imgRef = storage.child(fileName)
                    
                    try {
                        val uploadTask = imgRef.putFile(imageUri)
                        imageUrl = uploadTask.continueWithTask { task ->
                            if (!task.isSuccessful) {
                                task.exception?.let { throw it }
                            }
                            imgRef.downloadUrl
                        }.await().toString()
                    } catch (e: Exception) {
                        _state.value = InventoryState.Error("Error al subir la imagen: ${e.message}")
                        return@launch
                    }
                }

                val productData = hashMapOf<String, Any>(
                    "nombre" to nombre,
                    "descripcion" to descripcion,
                    "precioVenta" to precio,
                    "costoProduccion" to costo,
                    "stock" to stock,
                    "codigoBarras" to codigoBarras
                )
                
                if (imageUrl != null) {
                    productData["urlsMultimedia"] = listOf(imageUrl)
                }

                if (productId.isNullOrEmpty()) {
                    // Create
                    db.collection("productos").add(productData)
                        .addOnSuccessListener {
                            _state.value = InventoryState.Success
                        }
                        .addOnFailureListener { e ->
                            _state.value = InventoryState.Error(e.message ?: "Error al guardar")
                        }
                } else {
                    // Update
                    db.collection("productos").document(productId).update(productData)
                        .addOnSuccessListener {
                            _state.value = InventoryState.Success
                        }
                        .addOnFailureListener { e ->
                            _state.value = InventoryState.Error(e.message ?: "Error al actualizar")
                        }
                }
            } catch (e: Exception) {
                _state.value = InventoryState.Error(e.message ?: "Error al guardar producto")
            }
        }
    }
    
    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            _state.value = InventoryState.Loading
            try {
                db.collection("productos").document(productId).delete().await()
                _state.value = InventoryState.Success
            } catch (e: Exception) {
                _state.value = InventoryState.Error(e.message ?: "Error al eliminar producto")
            }
        }
    }
    
    fun resetState() {
        _state.value = InventoryState.Idle
    }
}
