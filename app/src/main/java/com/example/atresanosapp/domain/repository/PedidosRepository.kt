package com.example.atresanosapp.domain.repository

import com.example.atresanosapp.data.model.Pedido
import kotlinx.coroutines.flow.Flow

interface PedidosRepository {
    suspend fun crearPedido(pedido: Pedido): Result<String>
    suspend fun actualizarEstadoPedido(idPedido: String, estado: com.example.atresanosapp.data.model.EstadoPedido): Result<Unit>
    suspend fun asignarUrlPdfPedido(idPedido: String, url: String): Result<Unit>
    suspend fun obtenerPedidoPorId(idPedido: String): Result<Pedido?>
    fun obtenerPedidosUsuario(idUsuario: String): Flow<List<Pedido>>
    fun obtenerTodosLosPedidos(): Flow<List<Pedido>>
}
