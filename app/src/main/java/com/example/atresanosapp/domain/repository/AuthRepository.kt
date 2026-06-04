package com.example.atresanosapp.domain.repository

import com.example.atresanosapp.data.model.Usuario
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun registrarConCorreo(email: String, contrasenia: String, nombre: String, telefono: String): Result<Usuario>
    suspend fun iniciarSesionConCorreo(email: String, contrasenia: String): Result<Usuario>
    suspend fun guardarUsuarioEnFirestore(usuario: Usuario): Result<Unit>
    suspend fun obtenerUsuarioActual(): Result<Usuario?>
    fun cerrarSesion()
    
    // Las firmas para Google y Teléfono pueden requerir tokens o credenciales del UI
    suspend fun iniciarSesionConCredencialGoogle(idToken: String): Result<Usuario>
    suspend fun iniciarSesionConCredencialTelefono(verificationId: String, code: String): Result<Usuario>
}
