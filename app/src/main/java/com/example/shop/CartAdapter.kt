package com.example.shop

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CartAdapter(
    private val items: List<CartItem>,
    private val onDeleteClick: (CartItem) -> Unit,
    private val onQuantityChange: (CartItem, Int) -> Unit // НОВАЯ ФУНКЦИЯ
) : RecyclerView.Adapter<CartAdapter.CartHolder>() {

    class CartHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.cartName)
        val price: TextView = view.findViewById(R.id.cartPrice)
        val image: ImageView = view.findViewById(R.id.cartImage)
        val deleteBtn: ImageButton = view.findViewById(R.id.btnDeleteCart)

        // Элементы для изменения количества (убедись, что они есть в item_cart.xml)
        val btnMinus: ImageButton? = view.findViewById(R.id.btnMinus)
        val btnPlus: ImageButton? = view.findViewById(R.id.btnPlus)
        val tvQuantity: TextView? = view.findViewById(R.id.tvQuantity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return CartHolder(view)
    }

    override fun onBindViewHolder(holder: CartHolder, position: Int) {
        val item = items[position]

        holder.name.text = item.name
        holder.price.text = "${item.price * item.quantity} $" // Цена за ВСЕ количество

        if (item.imageUri != null) {
            holder.image.setImageURI(Uri.parse(item.imageUri))
        } else {
            holder.image.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        holder.deleteBtn.setOnClickListener { onDeleteClick(item) }

        // Логика кнопок + и -
        holder.tvQuantity?.text = item.quantity.toString()

        holder.btnMinus?.setOnClickListener {
            if (item.quantity > 1) {
                onQuantityChange(item, item.quantity - 1)
            } else {
                // Если количество 1 и нажали минус - можно спросить про удаление,
                // но пока просто ничего не делаем или вызываем удаление
                onDeleteClick(item)
            }
        }

        holder.btnPlus?.setOnClickListener {
            onQuantityChange(item, item.quantity + 1)
        }
    }

    override fun getItemCount() = items.size
}