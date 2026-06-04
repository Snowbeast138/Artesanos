package com.example.atresanosapp.presentation.admin.users

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.example.atresanosapp.data.model.Usuario
import com.example.atresanosapp.databinding.ItemUserBinding

class UserAdapter(
    private val onOptionsClick: (Usuario, String) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private var users = listOf<Usuario>()

    fun submitList(newList: List<Usuario>) {
        users = newList
        notifyDataSetChanged()
    }

    inner class UserViewHolder(private val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: Usuario) {
            binding.tvUserName.text = if (user.nombre.isNotBlank()) user.nombre else "Usuario Sin Nombre"
            binding.tvUserEmail.text = user.email
            binding.tvUserRole.text = "Rol: ${user.rol.name}"
            
            if (user.activo) {
                binding.tvUserStatus.text = "ACTIVO"
                binding.tvUserStatus.setTextColor(Color.parseColor("#4CAF50"))
            } else {
                binding.tvUserStatus.text = "DADO DE BAJA"
                binding.tvUserStatus.setTextColor(Color.parseColor("#F44336"))
            }

            binding.btnUserOptions.setOnClickListener { view ->
                val popup = PopupMenu(view.context, binding.btnUserOptions)
                popup.menu.add(0, 1, 0, "Hacer ADMIN")
                popup.menu.add(0, 2, 0, "Hacer DEV")
                popup.menu.add(0, 3, 0, "Hacer CLIENTE")
                
                if (user.activo) {
                    popup.menu.add(0, 4, 0, "Dar de Baja")
                } else {
                    popup.menu.add(0, 5, 0, "Reactivar Usuario")
                }
                
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        1 -> onOptionsClick(user, "ADMIN")
                        2 -> onOptionsClick(user, "DEV")
                        3 -> onOptionsClick(user, "CLIENTE")
                        4 -> onOptionsClick(user, "BAJA")
                        5 -> onOptionsClick(user, "ALTA")
                    }
                    true
                }
                popup.show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size
}
