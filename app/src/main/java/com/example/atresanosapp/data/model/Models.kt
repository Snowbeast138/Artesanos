package com.example.atresanosapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

// 1. Colección: usuarios (y subcolección: direcciones)
data class Usuario(
    @DocumentId val id: String = "",
    val nombre: String = "",
    val telefono: String = "",
    val email: String = "",
    val proveedorAuth: ProveedorAuth = ProveedorAuth.CORREO,
    val rol: RolUsuario = RolUsuario.CLIENTE,
    val activo: Boolean = true
)

enum class ProveedorAuth { CORREO, GOOGLE, TELEFONO }
enum class RolUsuario { DEV, CLIENTE, ADMIN }

data class Direccion(
    @DocumentId val id: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val direccionTexto: String = ""
)

// 2. Colección: productos
data class Producto(
    @DocumentId val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val precioVenta: Double = 0.0,
    val costoProduccion: Double = 0.0,
    val stock: Int = 0, // Control de disponibilidad
    val urlsMultimedia: List<String> = emptyList(),
    val codigoBarras: String = "",
    val jsonNutrimental: String = "" 
)

// 3. Colección: pedidos
data class Pedido(
    @DocumentId val id: String = "",
    val idUsuario: String = "",
    val nombreCliente: String = "", // Para vistas rápidas
    val productos: List<ProductoPedido> = emptyList(),
    val estado: EstadoPedido = EstadoPedido.SOLICITADO,
    val metodoPago: MetodoPago = MetodoPago.EFECTIVO,
    val latitudEntrega: Double = 0.0,
    val longitudEntrega: Double = 0.0,
    val fechaCreacion: Timestamp = Timestamp.now(),
    val fechaHoraMaximaEntrega: Timestamp? = null,
    val costoBruto: Double = 0.0, 
    val descuentoFijo: Double = 0.0, // Descuento en MXN (ej. 50.0)
    val descuentoPorcentaje: Double = 0.0, // Descuento en % (ej. 15.0)
    val costoNeto: Double = 0.0, // costoBruto - descuentoFijo - (costoBruto * descuentoPorcentaje / 100)
    val montoAbonado: Double = 0.0, // Total pagado hasta el momento
    val urlPdfRecibo: String = "", // URL del PDF almacenado en Firebase Storage
    val canceladoPor: String = "" // Nombre del usuario o admin que canceló
)

data class ProductoPedido(
    @DocumentId val id: String = "",
    val idProducto: String = "",
    val nombre: String = "", // Útil para el recibo PDF
    val cantidad: Int = 1,
    val precioUnitario: Double = 0.0,
    val cantidadEntregada: Int = 0
)

enum class EstadoPedido { SOLICITADO, VERIFICADO, EN_RUTA, ENTREGADO, CANCELADO }
enum class MetodoPago { EFECTIVO, TARJETA }

// 4. Colección: rutas_delivery
// DEV y ADMIN gestionan las rutas y asignan los pedidos para entrega.
data class RutaDelivery(
    @DocumentId val id: String = "",
    val fecha: Timestamp = Timestamp.now(),
    val asignadoA: String = "", // ID del Admin o Dev que entregará (o repartidor temporal)
    val ordenPedidosIds: List<String> = emptyList() 
)

// 5. Colección: estadisticas_ventas
data class EstadisticasVentas(
    @DocumentId val idProducto: String = "",
    val unidadesVendidas: Int = 0,
    val ingresosBrutos: Double = 0.0,
    val ingresosNetos: Double = 0.0
)

// 6. Colección: notificaciones
data class Notificacion(
    @DocumentId val id: String = "",
    val destinatarioId: String = "",
    val tipo: TipoNotificacion = TipoNotificacion.PEDIDO_NUEVO,
    val mensaje: String = "",
    val leida: Boolean = false,
    val fecha: Timestamp = Timestamp.now()
)

enum class TipoNotificacion { PEDIDO_NUEVO, TIEMPO_AGOTADO, PEDIDO_CONFIRMADO, REPARTIDOR_LLEGANDO, RECIBO_GENERADO }
