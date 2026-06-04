package com.example.atresanosapp.presentation.common.orders

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.atresanosapp.R
import com.example.atresanosapp.data.model.RolUsuario
import com.example.atresanosapp.databinding.FragmentOrderDetailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope

class OrderDetailFragment : Fragment() {
    private var _binding: FragmentOrderDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrderDetailViewModel by viewModels()
    private lateinit var productAdapter: OrderProductAdapter

    private var pedidoId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pedidoId = arguments?.getString("pedidoId")
        if (pedidoId.isNullOrEmpty()) {
            findNavController().navigateUp()
            return
        }

        binding.toolbarOrderDetail.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        productAdapter = OrderProductAdapter(emptyList(), false) { _, _ -> }
        binding.rvDetailProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDetailProducts.adapter = productAdapter

        binding.btnCancelOrder.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Cancelar Pedido")
                .setMessage("¿Estás seguro de que deseas cancelar este pedido? Los productos regresarán al inventario.")
                .setPositiveButton("Sí, Cancelar") { _, _ ->
                    val pId = pedidoId
                    val productos = viewModel.pedido.value?.productos
                    if (pId != null && productos != null) {
                        binding.btnCancelOrder.isEnabled = false
                        viewModel.cancelarPedido(pId, productos) {
                            android.widget.Toast.makeText(context, "Pedido cancelado exitosamente", android.widget.Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        }
                    }
                }
                .setNegativeButton("No", null)
                .show()
        }

        checkUserRoleAndConfigureActions()
        viewModel.loadOrder(pedidoId!!)

        lifecycleScope.launchWhenStarted {
            viewModel.pedido.collect { pedido ->
                pedido?.let {
                    binding.tvDetailOrderId.text = "Pedido: #${it.id.takeLast(6).uppercase()}"
                    binding.tvDetailStatus.text = "Estado: ${it.estado.name}"
                    
                    if (it.estado == com.example.atresanosapp.data.model.EstadoPedido.ENTREGADO || it.estado == com.example.atresanosapp.data.model.EstadoPedido.CANCELADO) {
                        binding.btnAdminAbonar.visibility = View.GONE
                        binding.btnAdminLiquidar.visibility = View.GONE
                        binding.btnAdminDescuento.visibility = View.GONE
                        binding.btnCancelOrder.visibility = View.GONE
                        
                        if (it.estado == com.example.atresanosapp.data.model.EstadoPedido.CANCELADO) {
                            binding.btnAdminEnviarNota.visibility = View.GONE
                        } else {
                            binding.btnAdminEnviarNota.text = "Descargar Nota / Enviar PDF"
                        }
                    } else {
                        binding.btnAdminAbonar.visibility = View.VISIBLE
                        binding.btnAdminLiquidar.visibility = View.VISIBLE
                        binding.btnAdminDescuento.visibility = View.VISIBLE
                        binding.btnCancelOrder.visibility = View.VISIBLE
                        binding.btnAdminEnviarNota.text = "Enviar Nota (PDF)"
                    }
                    
                    val dateStr = it.fechaHoraMaximaEntrega?.toDate()?.let { date ->
                        java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a", java.util.Locale.getDefault()).format(date)
                    } ?: "No especificada"
                    binding.tvDetailDate.text = "Fecha Entrega Máx: $dateStr"
                    
                    val lat = it.latitudEntrega
                    val lng = it.longitudEntrega
                    binding.tvDetailAddress.text = "Calculando dirección..."

                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        try {
                            val geocoder = android.location.Geocoder(requireContext(), java.util.Locale.getDefault())
                            val addresses = geocoder.getFromLocation(lat, lng, 1)
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                if (!addresses.isNullOrEmpty()) {
                                    val address = addresses[0]
                                    val addressText = address.getAddressLine(0) ?: "Lat $lat, Lng $lng"
                                    binding.tvDetailAddress.text = "Domicilio: $addressText"
                                } else {
                                    binding.tvDetailAddress.text = "Domicilio: Lat $lat, Lng $lng"
                                }
                            }
                        } catch (e: Exception) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                binding.tvDetailAddress.text = "Domicilio: Lat $lat, Lng $lng"
                            }
                        }
                    }

                    val subtotal = it.costoBruto
                    val descuentoFijo = it.descuentoFijo
                    val total = it.costoNeto
                    val abonado = it.montoAbonado
                    val deuda = total - abonado

                    binding.tvDetailGross.text = "Subtotal: $${String.format("%.2f", subtotal)}"
                    binding.tvDetailDiscount.text = "Descuento Fijo: -$${String.format("%.2f", descuentoFijo)}"
                    binding.tvDetailNet.text = "Total a Pagar: $${String.format("%.2f", total)}"
                    binding.tvDetailPaid.text = "Abonado: $${String.format("%.2f", abonado)}"
                    binding.tvDetailDebt.text = "Deuda: $${String.format("%.2f", deuda)}"

                    if (abonado >= total) {
                        binding.tvDetailPaymentStatus.text = "Pago: Pagado"
                        binding.tvDetailPaymentStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
                    } else if (abonado > 0) {
                        binding.tvDetailPaymentStatus.text = "Pago: Pendiente"
                        binding.tvDetailPaymentStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark))
                    } else {
                        binding.tvDetailPaymentStatus.text = "Pago: Sin Pago"
                        binding.tvDetailPaymentStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                    }

                    productAdapter.submitList(it.productos)
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.cliente.collect { cliente ->
                cliente?.let {
                    binding.tvDetailClientName.text = "Cliente: ${it.nombre}"
                    binding.tvDetailClientPhone.text = "Teléfono: ${it.telefono}"
                }
            }
        }
    }

    private fun checkUserRoleAndConfigureActions() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                var isAdmin = false
                if (doc.exists()) {
                    val rolStr = doc.getString("rol") ?: ""
                    if (rolStr == RolUsuario.ADMIN.name || rolStr == RolUsuario.DEV.name) {
                        isAdmin = true
                        binding.layoutAdminActions.visibility = View.VISIBLE
                        setupAdminActions()
                    } else {
                        binding.layoutAdminActions.visibility = View.GONE
                    }
                }
                setupAdapter(isAdmin)
            }
            .addOnFailureListener {
                setupAdapter(false)
            }
    }

    private fun setupAdapter(isAdmin: Boolean) {
        productAdapter = OrderProductAdapter(emptyList(), isAdmin) { producto, _ ->
            val pedidoValue = viewModel.pedido.value
            val currentList = pedidoValue?.productos ?: return@OrderProductAdapter
            
            if (pedidoValue.estado == com.example.atresanosapp.data.model.EstadoPedido.ENTREGADO || pedidoValue.estado == com.example.atresanosapp.data.model.EstadoPedido.CANCELADO) {
                android.widget.Toast.makeText(context, "El pedido no puede ser modificado (Entregado o Cancelado)", android.widget.Toast.LENGTH_SHORT).show()
                return@OrderProductAdapter
            }
            pedidoId?.let { pid ->
                val builder = android.app.AlertDialog.Builder(requireContext())
                builder.setTitle("Piezas Entregadas")
                builder.setMessage("¿Cuántas piezas de '${producto.nombre}' se han entregado?")
                
                val input = android.widget.EditText(requireContext())
                input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                input.setText(producto.cantidadEntregada.toString())
                builder.setView(input)

                builder.setPositiveButton("Guardar") { dialog, _ ->
                    val cantidad = input.text.toString().toIntOrNull() ?: 0
                    val cantidadFinal = if (cantidad > producto.cantidad) producto.cantidad else if (cantidad < 0) 0 else cantidad
                    viewModel.setProductoEntregado(pid, producto, cantidadFinal, currentList)
                    dialog.dismiss()
                }
                builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
                builder.show()
            }
        }
        binding.rvDetailProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDetailProducts.adapter = productAdapter
        
        viewModel.pedido.value?.productos?.let { productAdapter.submitList(it) }
    }

    private fun setupAdminActions() {
        binding.btnAdminAbonar.setOnClickListener {
            showInputDialog("Abonar Dinero", "Ingresa la cantidad a abonar") { cantidad ->
                pedidoId?.let { viewModel.abonarDinero(it, cantidad) }
            }
        }
        binding.btnAdminLiquidar.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Liquidar Pedido")
                .setMessage("¿Estás seguro de que deseas marcar este pedido como pagado en su totalidad?")
                .setPositiveButton("Liquidar") { _, _ ->
                    pedidoId?.let { viewModel.liquidarPedido(it) }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
        binding.btnAdminDescuento.setOnClickListener {
            showInputDialog("Aplicar Descuento", "Ingresa el descuento en MXN") { descuento ->
                pedidoId?.let { viewModel.aplicarDescuento(it, descuento) }
            }
        }
        binding.btnAdminEnviarNota.setOnClickListener {
            val cliente = viewModel.cliente.value
            val pedido = viewModel.pedido.value
            if (cliente == null || pedido == null) {
                android.widget.Toast.makeText(context, "Información no disponible aún", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val options = mutableListOf<String>()
            if (cliente.telefono.isNotEmpty()) options.add("WhatsApp")
            if (cliente.email.isNotEmpty()) options.add("Correo Electrónico")

            if (options.isEmpty()) {
                android.widget.Toast.makeText(context, "El cliente no tiene teléfono ni correo registrado.", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Enviar Factura/Nota a través de:")
                .setItems(options.toTypedArray()) { _, which ->
                    val method = if (options[which] == "WhatsApp") "WhatsApp" else "Email"
                    com.example.atresanosapp.utils.PdfHelper.generateAndShareInvoice(
                        requireContext(),
                        pedido,
                        cliente,
                        method
                    )
                }
                .show()
        }
    }

    private fun showInputDialog(title: String, message: String, onConfirm: (Double) -> Unit) {
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setView(input)
            .setPositiveButton("Confirmar") { _, _ ->
                val text = input.text.toString()
                if (text.isNotEmpty()) {
                    onConfirm(text.toDoubleOrNull() ?: 0.0)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
