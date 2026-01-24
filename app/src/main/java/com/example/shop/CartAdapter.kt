package com.example.shop

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CartAdapter(
    private val items: List<CartItem>,
    private val onPlusClick: (CartItem) -> Unit,
    private val onMinusClick: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartHolder>() {

    class CartHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.cartName)
        val price: TextView = view.findViewById(R.id.cartPrice)
        val image: ImageView = view.findViewById(R.id.cartImage)
        val quantity: TextView = view.findViewById(R.id.tvQuantity)
        val btnPlus: Button = view.findViewById(R.id.btnPlus)
        val btnMinus: Button = view.findViewById(R.id.btnMinus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return CartHolder(view)
    }

    override fun onBindViewHolder(holder: CartHolder, position: Int) {
        val item = items[position]

        holder.name.text = item.name
        holder.price.text = "${item.price * item.quantity} $" // Показываем общую цену
        holder.quantity.text = item.quantity.toString()

        if (item.imageUri != null) {
            holder.image.setImageURI(Uri.parse(item.imageUri))
        } else {
            holder.image.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        holder.btnPlus.setOnClickListener { onPlusClick(item) }
        holder.btnMinus.setOnClickListener { onMinusClick(item) }
    }

    override fun getItemCount(): Int = items.size
}