package com.example.atresanosapp.data.repository

import com.example.atresanosapp.data.model.Pedido
import com.example.atresanosapp.domain.repository.PedidosRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PedidosRepositoryImpl(
    private val firestore: FirebaseFirestore
) : PedidosRepository {

    private val pedidosCollection = firestore.collection("pedidos")

    override suspend fun crearPedido(pedido: Pedido): Result<String> {
        return try {
            val docRef = pedidosCollection.document()
            val nuevoPedido = pedido.copy(id = docRef.id)
            docRef.set(nuevoPedido).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun actualizarEstadoPedido(idPedido: String, estado: com.example.atresanosapp.data.model.EstadoPedido): Result<Unit> {
        return try {
            pedidosCollection.document(idPedido).update("estado", estado.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun asignarUrlPdfPedido(idPedido: String, url: String): Result<Unit> {
        return try {
            pedidosCollection.document(idPedido).update("urlPdfRecibo", url).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun obtenerPedidoPorId(idPedido: String): Result<Pedido?> {
        return try {
            val doc = pedidosCollection.document(idPedido).get().await()
            val pedido = doc.toObject(Pedido::class.java)
            Result.success(pedido)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun obtenerPedidosUsuario(idUsuario: String): Flow<List<Pedido>> = callbackFlow {
        val listenerRegistration = pedidosCollection
            .whereEqualTo("idUsuario", idUsuario)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val pedidos = snapshot.documents.mapNotNull { it.toObject(Pedido::class.java) }
                    trySend(pedidos)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    override fun obtenerTodosLosPedidos(): Flow<List<Pedido>> = callbackFlow {
        val listenerRegistration = pedidosCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val pedidos = snapshot.documents.mapNotNull { it.toObject(Pedido::class.java) }
                    trySend(pedidos)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }
}
