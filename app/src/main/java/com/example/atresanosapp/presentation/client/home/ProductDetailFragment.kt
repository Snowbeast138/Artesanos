package com.example.atresanosapp.presentation.client.home

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.example.atresanosapp.data.model.Producto
import com.example.atresanosapp.databinding.FragmentProductDetailBinding
import com.google.firebase.firestore.FirebaseFirestore

class ProductDetailFragment : Fragment() {
    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!

    private val cartViewModel: com.example.atresanosapp.presentation.client.cart.CartViewModel by activityViewModels()

    private var productId: String? = null
    private var productoActual: Producto? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        productId = arguments?.getString("productId")

        if (productId != null) {
            loadProduct(productId!!)
        } else {
            Toast.makeText(context, "Producto no encontrado", Toast.LENGTH_SHORT).show()
        }

        binding.btnMinus.setOnClickListener {
            var currentQty = binding.etQuantity.text.toString().toIntOrNull() ?: 1
            if (currentQty > 1) {
                currentQty--
                binding.etQuantity.setText(currentQty.toString())
            }
        }

        binding.btnPlus.setOnClickListener {
            val maxStock = productoActual?.stock ?: 1
            var currentQty = binding.etQuantity.text.toString().toIntOrNull() ?: 1
            if (currentQty < maxStock) {
                currentQty++
                binding.etQuantity.setText(currentQty.toString())
            } else {
                Toast.makeText(context, "Stock máximo alcanzado", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnDetailAddCart.setOnClickListener {
            productoActual?.let { prod ->
                if (prod.stock > 0) {
                    var desiredQty = binding.etQuantity.text.toString().toIntOrNull() ?: 1
                    if (desiredQty < 1) desiredQty = 1
                    if (desiredQty > prod.stock) {
                        desiredQty = prod.stock
                        binding.etQuantity.setText(desiredQty.toString())
                        Toast.makeText(context, "Solo hay ${prod.stock} disponibles", Toast.LENGTH_SHORT).show()
                    }
                    
                    val productoPedido = com.example.atresanosapp.data.model.ProductoPedido(
                        idProducto = prod.id,
                        nombre = prod.nombre,
                        cantidad = desiredQty,
                        precioUnitario = prod.precioVenta
                    )
                    cartViewModel.agregarAlCarrito(productoPedido)
                    Toast.makeText(context, "${prod.nombre} añadido al carrito ($desiredQty uds)", Toast.LENGTH_SHORT).show()
                    
                    com.example.atresanosapp.utils.NotificationHelper.showNotification(
                        requireContext(),
                        "Carrito de Compras",
                        "Agregaste ${prod.nombre} ($desiredQty) a tu carrito."
                    )
                }
            }
        }
    }

    private fun loadProduct(id: String) {
        FirebaseFirestore.getInstance().collection("productos").document(id).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val producto = doc.toObject(Producto::class.java)?.copy(id = doc.id)
                    producto?.let {
                        productoActual = it
                        binding.tvDetailName.text = it.nombre
                        binding.tvDetailPrice.text = "$${String.format("%.2f", it.precioVenta)}"
                        binding.tvDetailDesc.text = it.descripcion

                        if (it.stock > 0) {
                            binding.tvDetailStock.text = "En Stock: ${it.stock}"
                            binding.btnDetailAddCart.isEnabled = true
                            binding.btnDetailAddCart.text = "Añadir al Carrito"
                            binding.btnDetailAddCart.alpha = 1.0f
                        } else {
                            binding.tvDetailStock.text = "Agotado"
                            binding.tvDetailStock.setTextColor(Color.RED)
                            binding.btnDetailAddCart.isEnabled = false
                            binding.btnDetailAddCart.text = "Producto Agotado"
                            binding.btnDetailAddCart.alpha = 0.5f
                        }

                        it.urlsMultimedia.firstOrNull()?.let { url ->
                            Glide.with(this).load(url).into(binding.ivDetailImage)
                        }
                    }
                } else {
                    Toast.makeText(context, "Producto no disponible", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al cargar el producto", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
