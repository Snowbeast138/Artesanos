package com.example.atresanosapp.presentation.admin.inventory

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.atresanosapp.databinding.FragmentAdminAddProductBinding
import kotlinx.coroutines.launch

class AdminAddProductFragment : Fragment() {
    private var _binding: FragmentAdminAddProductBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AdminInventoryViewModel
    
    private var selectedImageUri: Uri? = null
    private var existingProductId: String? = null
    private var existingImageUrl: String? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            Glide.with(this).load(uri).into(binding.ivProductImage)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminAddProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this)[AdminInventoryViewModel::class.java]

        checkArgumentsAndPopulate()

        binding.ivProductImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        parentFragmentManager.setFragmentResultListener("barcodeResult", viewLifecycleOwner) { _, bundle ->
            val result = bundle.getString("barcode")
            if (result != null) {
                binding.etCodigoBarras.setText(result)
            }
        }

        binding.btnScanBarcode.setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean("isForResult", true)
            }
            findNavController().navigate(com.example.atresanosapp.R.id.action_adminAddProductFragment_to_scannerFragment, bundle)
        }

        binding.btnGuardarProducto.setOnClickListener {
            val nombre = binding.etNombreProd.text.toString()
            val desc = binding.etDescProd.text.toString()
            val precio = binding.etPrecioProd.text.toString().toDoubleOrNull() ?: 0.0
            val costo = binding.etCostoProd.text.toString().toDoubleOrNull() ?: 0.0
            val stock = binding.etStockProd.text.toString().toIntOrNull() ?: 0
            val codigoBarras = binding.etCodigoBarras.text.toString().trim()

            if (nombre.isNotBlank() && desc.isNotBlank() && precio > 0 && stock >= 0) {
                viewModel.addOrUpdateProduct(
                    existingProductId, nombre, desc, precio, costo, stock, codigoBarras, selectedImageUri, existingImageUrl
                )
            } else {
                Toast.makeText(context, "Llena todos los campos correctamente", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.btnEliminarProducto.setOnClickListener {
            existingProductId?.let { id ->
                viewModel.deleteProduct(id)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is InventoryState.Loading -> {
                        binding.btnGuardarProducto.isEnabled = false
                        binding.btnGuardarProducto.text = "Guardando..."
                    }
                    is InventoryState.Success -> {
                        Toast.makeText(context, "Operación exitosa", Toast.LENGTH_SHORT).show()
                        viewModel.resetState()
                        findNavController().navigateUp()
                    }
                    is InventoryState.Error -> {
                        binding.btnGuardarProducto.isEnabled = true
                        binding.btnGuardarProducto.text = "Guardar Producto"
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                    is InventoryState.ProductsLoaded -> {
                        // Sometimes the snapshot listener triggers before Success, we should re-enable the button just in case
                        binding.btnGuardarProducto.isEnabled = true
                        binding.btnGuardarProducto.text = "Guardar Producto"
                    }
                    else -> {
                        binding.btnGuardarProducto.isEnabled = true
                        binding.btnGuardarProducto.text = "Guardar Producto"
                    }
                }
            }
        }
    }

    private fun checkArgumentsAndPopulate() {
        arguments?.let { bundle ->
            existingProductId = bundle.getString("productId")
            if (existingProductId != null) {
                binding.tvTitle.text = "Editar Producto"
                binding.btnGuardarProducto.text = "Actualizar"
                binding.btnEliminarProducto.visibility = View.VISIBLE
                
                binding.etNombreProd.setText(bundle.getString("nombre"))
                binding.etDescProd.setText(bundle.getString("descripcion"))
                binding.etPrecioProd.setText(bundle.getDouble("precio").toString())
                binding.etCostoProd.setText(bundle.getDouble("costo").toString())
                binding.etStockProd.setText(bundle.getInt("stock").toString())
                binding.etCodigoBarras.setText(bundle.getString("codigoBarras") ?: "")
                
                existingImageUrl = bundle.getString("imageUrl")
                existingImageUrl?.let { url ->
                    Glide.with(this).load(url).into(binding.ivProductImage)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
