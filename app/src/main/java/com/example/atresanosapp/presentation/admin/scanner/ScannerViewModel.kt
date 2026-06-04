package com.example.atresanosapp.presentation.admin.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ScannerState {
    object Idle : ScannerState()
    object Processing : ScannerState()
    data class Success(val info: String) : ScannerState()
    data class Error(val error: String) : ScannerState()
}

class ScannerViewModel : ViewModel() {

    private val _scannerState = MutableStateFlow<ScannerState>(ScannerState.Idle)
    val scannerState: StateFlow<ScannerState> = _scannerState

    fun procesarCodigoBarras(codigo: String) {
        viewModelScope.launch {
            _scannerState.value = ScannerState.Processing
            // Aquí iría la lógica para buscar el código en Firestore (tabla nutrimental, etc.)
            // Simulamos respuesta
            _scannerState.value = ScannerState.Success("Código procesado: $codigo")
        }
    }

    fun validarEntregaQR(qrCode: String) {
        viewModelScope.launch {
            _scannerState.value = ScannerState.Processing
            // Aquí iría la lógica para marcar pedido como Entregado
            _scannerState.value = ScannerState.Success("QR Válido. Pedido entregado.")
        }
    }
}
