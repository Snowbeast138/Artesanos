package com.example.atresanosapp.data.repository

import com.example.atresanosapp.data.model.Usuario
import com.example.atresanosapp.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    private val usersCollection = firestore.collection("usuarios")

    override suspend fun registrarConCorreo(email: String, contrasenia: String, nombre: String, telefono: String): Result<Usuario> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, contrasenia).await()
            val firebaseUser = result.user ?: throw Exception("Error al crear usuario")
            
            val nuevoUsuario = Usuario(
                id = firebaseUser.uid,
                nombre = nombre,
                telefono = telefono,
                email = email,
                proveedorAuth = com.example.atresanosapp.data.model.ProveedorAuth.CORREO,
                rol = com.example.atresanosapp.data.model.RolUsuario.CLIENTE
            )
            guardarUsuarioEnFirestore(nuevoUsuario)
            Result.success(nuevoUsuario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun iniciarSesionConCorreo(email: String, contrasenia: String): Result<Usuario> {
        return try {
            auth.signInWithEmailAndPassword(email, contrasenia).await()
            val result = obtenerUsuarioActual()
            if (result.isSuccess) {
                val usuario = result.getOrNull()
                if (usuario != null && !usuario.activo) {
                    auth.signOut()
                    throw Exception("Cuenta dada de baja. Contacta a soporte.")
                }
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun guardarUsuarioEnFirestore(usuario: Usuario): Result<Unit> {
        return try {
            usersCollection.document(usuario.id).set(usuario).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun obtenerUsuarioActual(): Result<Usuario> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("No hay sesión activa")
            val document = usersCollection.document(uid).get().await()
            val usuario = document.toObject(Usuario::class.java) ?: throw Exception("Usuario no encontrado en BD")
            Result.success(usuario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun cerrarSesion() {
        auth.signOut()
    }

    override suspend fun iniciarSesionConCredencialGoogle(idToken: String): Result<Usuario> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw Exception("Error de autenticación Google")
            
            // Verificamos si ya existe en Firestore
            val doc = usersCollection.document(firebaseUser.uid).get().await()
            if (!doc.exists()) {
                val nuevoUsuario = Usuario(
                    id = firebaseUser.uid,
                    nombre = firebaseUser.displayName ?: "",
                    telefono = firebaseUser.phoneNumber ?: "",
                    email = firebaseUser.email ?: "",
                    proveedorAuth = com.example.atresanosapp.data.model.ProveedorAuth.GOOGLE,
                    rol = com.example.atresanosapp.data.model.RolUsuario.CLIENTE
                )
                guardarUsuarioEnFirestore(nuevoUsuario)
                Result.success(nuevoUsuario)
            } else {
                val usuario = doc.toObject(Usuario::class.java)!!
                if (!usuario.activo) {
                    auth.signOut()
                    throw Exception("Cuenta dada de baja. Contacta a soporte.")
                }
                Result.success(usuario)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun iniciarSesionConCredencialTelefono(verificationId: String, code: String): Result<Usuario> {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw Exception("Error de autenticación Telefónica")
            
            val doc = usersCollection.document(firebaseUser.uid).get().await()
            if (!doc.exists()) {
                val nuevoUsuario = Usuario(
                    id = firebaseUser.uid,
                    nombre = "Usuario Nuevo", // A completar en perfil
                    telefono = firebaseUser.phoneNumber ?: "",
                    email = "", // A completar
                    proveedorAuth = com.example.atresanosapp.data.model.ProveedorAuth.TELEFONO,
                    rol = com.example.atresanosapp.data.model.RolUsuario.CLIENTE
                )
                guardarUsuarioEnFirestore(nuevoUsuario)
                Result.success(nuevoUsuario)
            } else {
                val usuario = doc.toObject(Usuario::class.java)!!
                if (!usuario.activo) {
                    auth.signOut()
                    throw Exception("Cuenta dada de baja. Contacta a soporte.")
                }
                Result.success(usuario)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
