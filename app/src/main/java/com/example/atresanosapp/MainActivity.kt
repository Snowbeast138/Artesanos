package com.example.atresanosapp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.atresanosapp.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.atresanosapp.data.model.EstadoPedido
import com.example.atresanosapp.data.model.Pedido
import com.example.atresanosapp.utils.NotificationHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        NotificationHelper.createNotificationChannel(this)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val lastPagos = mutableMapOf<String, Double>()
            var initialRole: String? = null
            var adminOrdersListener: com.google.firebase.firestore.ListenerRegistration? = null
            var clientOrdersListener: com.google.firebase.firestore.ListenerRegistration? = null
            
            // Listen for user document changes
            FirebaseFirestore.getInstance().collection("usuarios").document(currentUser.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    
                    val usuario = snapshot.toObject(com.example.atresanosapp.data.model.Usuario::class.java)
                    if (usuario != null) {
                        if (!usuario.activo) {
                            FirebaseAuth.getInstance().signOut()
                            android.widget.Toast.makeText(this, "Tu cuenta ha sido dada de baja.", android.widget.Toast.LENGTH_LONG).show()
                            navController.navigate(R.id.loginFragment)
                            return@addSnapshotListener
                        }
                        
                        val currentRole = usuario.rol.name
                        val isAdmin = currentRole == "ADMIN" || currentRole == "DEV"
                        
                        if (initialRole == null) {
                            initialRole = currentRole
                            
                            // Configurar Bottom Navigation la primera vez
                            if (isAdmin) {
                                binding.bottomNavigation.menu.clear()
                                binding.bottomNavigation.inflateMenu(R.menu.bottom_nav_menu_admin)
                            }
                            binding.bottomNavigation.setupWithNavController(navController)

                            // Configurar Listener de Pedidos según Rol
                            if (isAdmin) {
                                // ADMIN LISTENER: Listen to ALL orders to detect if a client cancels
                                adminOrdersListener = FirebaseFirestore.getInstance().collection("pedidos")
                                    .addSnapshotListener { orderSnapshot, orderError ->
                                        if (orderError != null || orderSnapshot == null) return@addSnapshotListener
                                        for (change in orderSnapshot.documentChanges) {
                                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.MODIFIED) {
                                                val pedido = change.document.toObject(Pedido::class.java)
                                                if (pedido.estado == EstadoPedido.CANCELADO) {
                                                    val dateStr = pedido.fechaCreacion.toDate().let { date ->
                                                        java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(date)
                                                    }
                                                    NotificationHelper.showNotification(
                                                        this@MainActivity,
                                                        "Pedido Cancelado",
                                                        "El usuario ${pedido.canceladoPor} canceló el pedido del $dateStr.",
                                                        pedido.id.hashCode()
                                                    )
                                                }
                                            }
                                        }
                                    }
                            }

                            // CLIENT LISTENER (y Admin de sus propios pedidos)
                            clientOrdersListener = FirebaseFirestore.getInstance().collection("pedidos")
                                .whereEqualTo("idUsuario", currentUser.uid)
                                .addSnapshotListener { orderSnapshot, orderError ->
                                    if (orderError != null || orderSnapshot == null) return@addSnapshotListener
                                    
                                    for (change in orderSnapshot.documentChanges) {
                                        val pedido = change.document.toObject(Pedido::class.java)
                                        
                                        if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                            lastPagos[pedido.id] = pedido.montoAbonado
                                        }
                                        else if (change.type == com.google.firebase.firestore.DocumentChange.Type.MODIFIED) {
                                            if (pedido.estado == EstadoPedido.ENTREGADO) {
                                                NotificationHelper.showNotification(
                                                    this, "¡Pedido Entregado!",
                                                    "Tu pedido #${pedido.id.takeLast(6).uppercase()} ha sido entregado.",
                                                    pedido.id.hashCode()
                                                )
                                            } else if (pedido.estado == EstadoPedido.CANCELADO) {
                                                // Solo notificar si no es admin (si es admin, ya se notificó arriba en general)
                                                if (!isAdmin) {
                                                    NotificationHelper.showNotification(
                                                        this, "Pedido Cancelado",
                                                        "Tu pedido #${pedido.id.takeLast(6).uppercase()} ha sido cancelado y reembolsado al inventario.",
                                                        pedido.id.hashCode()
                                                    )
                                                }
                                            }
                                            
                                            val oldPago = lastPagos[pedido.id] ?: 0.0
                                            if (pedido.montoAbonado > oldPago) {
                                                val abonoDiff = pedido.montoAbonado - oldPago
                                                NotificationHelper.showNotification(
                                                    this, "Abono Registrado",
                                                    "Se han abonado $${String.format("%.2f", abonoDiff)} a tu pedido #${pedido.id.takeLast(6).uppercase()}.",
                                                    (pedido.id.hashCode() + 1)
                                                )
                                            }
                                            lastPagos[pedido.id] = pedido.montoAbonado
                                        }
                                        else if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                            lastPagos.remove(pedido.id)
                                        }
                                    }
                                }
                                
                        } else if (initialRole != currentRole) {
                            // Si el rol cambió en tiempo real, cerramos sesión
                            adminOrdersListener?.remove()
                            clientOrdersListener?.remove()
                            FirebaseAuth.getInstance().signOut()
                            android.widget.Toast.makeText(this, "Tus permisos han cambiado. Inicia sesión nuevamente.", android.widget.Toast.LENGTH_LONG).show()
                            navController.navigate(R.id.loginFragment)
                        }
                    }
                }
        } else {
            binding.bottomNavigation.setupWithNavController(navController)
        }

        // Ocultar el Bottom Navigation en pantallas de Login/Registro
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.loginFragment || destination.id == R.id.registerFragment) {
                binding.bottomNavigation.visibility = View.GONE
            } else {
                binding.bottomNavigation.visibility = View.VISIBLE
            }
        }
    }
}