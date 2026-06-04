package com.example.atresanosapp.presentation.admin.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.atresanosapp.databinding.FragmentScannerBinding
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerFragment : Fragment() {
    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcodeValue ->
                        // Detener la cámara momentáneamente
                        cameraProvider.unbindAll()
                        activity?.runOnUiThread {
                            processScannedCode(barcodeValue)
                        }
                    })
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer
                )
            } catch (exc: Exception) {
                Toast.makeText(context, "Error al iniciar cámara", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }

    private fun processScannedCode(barcodeValue: String) {
        val isForResult = arguments?.getBoolean("isForResult") ?: false
        if (isForResult) {
            val resultBundle = Bundle().apply {
                putString("barcode", barcodeValue)
            }
            parentFragmentManager.setFragmentResult("barcodeResult", resultBundle)
            findNavController().navigateUp()
            return
        }

        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        
        Toast.makeText(context, "Buscando $barcodeValue...", Toast.LENGTH_SHORT).show()

        db.collection("productos").whereEqualTo("codigoBarras", barcodeValue).get()
            .addOnSuccessListener { query ->
                if (!query.isEmpty) {
                    val doc = query.documents[0]
                    val productoId = doc.id
                    val nombre = doc.getString("nombre")
                    val descripcion = doc.getString("descripcion")
                    val precio = doc.getDouble("precioVenta") ?: 0.0
                    val costo = doc.getDouble("costoProduccion") ?: 0.0
                    val stock = doc.getDouble("stock")?.toInt() ?: 0
                    val codigo = doc.getString("codigoBarras")
                    val imageUrl = (doc.get("urlsMultimedia") as? List<*>)?.firstOrNull() as? String

                    val bundle = Bundle().apply {
                        putString("productId", productoId)
                        putString("nombre", nombre)
                        putString("descripcion", descripcion)
                        putDouble("precio", precio)
                        putDouble("costo", costo)
                        putInt("stock", stock)
                        putString("codigoBarras", codigo)
                        putString("imageUrl", imageUrl)
                    }

                    // Verificar Rol
                    auth.currentUser?.uid?.let { uid ->
                        db.collection("usuarios").document(uid).get().addOnSuccessListener { userDoc ->
                            val rol = userDoc.getString("rol")
                            if (rol == "ADMIN" || rol == "DEV") {
                                findNavController().navigate(com.example.atresanosapp.R.id.action_scannerFragment_to_adminAddProductFragment, bundle)
                            } else {
                                findNavController().navigate(com.example.atresanosapp.R.id.action_scannerFragment_to_productDetailFragment, bundle)
                            }
                        }
                    }
                } else {
                    Toast.makeText(context, "Producto no encontrado", Toast.LENGTH_SHORT).show()
                    startCamera() // Reiniciar cámara si no encontró
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al buscar", Toast.LENGTH_SHORT).show()
                startCamera()
            }
    }

    private class BarcodeAnalyzer(private val onBarcodeDetected: (String) -> Unit) : ImageAnalysis.Analyzer {
        private val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
        )

        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            barcode.rawValue?.let { value ->
                                onBarcodeDetected(value)
                                // Prevent multiple scannings immediately
                                imageProxy.close()
                                return@addOnSuccessListener
                            }
                        }
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }
}
