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
    private val cartItems: List<CartItem>,
    private val onDeleteClick: (CartItem) -> Unit,
    private val onQuantityChange: (CartItem, Int) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartHolder>() {

    class CartHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.cartName)
        val price: TextView = view.findViewById(R.id.cartPrice)
        val quantity: TextView = view.findViewById(R.id.tvQuantity)
        val image: ImageView = view.findViewById(R.id.cartImage)
        val btnPlus: ImageButton = view.findViewById(R.id.btnPlus)
        val btnMinus: ImageButton = view.findViewById(R.id.btnMinus)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteCart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return CartHolder(view)
    }

    override fun onBindViewHolder(holder: CartHolder, position: Int) {
        val item = cartItems[position]

        holder.name.text = item.name

        // ИСПРАВЛЕНИЕ 2: Убираем "хвосты" после запятой (toInt())
        // Показываем цену за ВСЕ количество сразу (цена * кол-во)
        val currentTotalPrice = item.price * item.quantity
        holder.price.text = "${currentTotalPrice.toInt()} ₽"

        holder.quantity.text = item.quantity.toString()

        if (item.imageUri != null) {
            holder.image.setImageURI(Uri.parse(item.imageUri))
            holder.image.scaleType = ImageView.ScaleType.CENTER_CROP
        } else {
            holder.image.setImageResource(android.R.drawable.ic_menu_camera)
            holder.image.scaleType = ImageView.ScaleType.FIT_CENTER
            holder.image.setPadding(20, 20, 20, 20)
        }

        holder.btnDelete.setOnClickListener { onDeleteClick(item) }

        // --- ЛОГИКА ПЛЮСА ---
        holder.btnPlus.setOnClickListener {
            item.quantity++ // Увеличиваем в памяти

            // ИСПРАВЛЕНИЕ 1: Мгновенно обновляем текст на экране
            holder.quantity.text = item.quantity.toString()
            holder.price.text = "${(item.price * item.quantity).toInt()} ₽"

            // Сообщаем базе данных
            onQuantityChange(item, item.quantity)
        }

        // --- ЛОГИКА МИНУСА ---
        holder.btnMinus.setOnClickListener {
            if (item.quantity > 1) {
                item.quantity-- // Уменьшаем в памяти

                // ИСПРАВЛЕНИЕ 1: Мгновенно обновляем текст на экране
                holder.quantity.text = item.quantity.toString()
                holder.price.text = "${(item.price * item.quantity).toInt()} ₽"

                // Сообщаем базе данных
                onQuantityChange(item, item.quantity)
            }
        }
    }

    override fun getItemCount(): Int = cartItems.size
}