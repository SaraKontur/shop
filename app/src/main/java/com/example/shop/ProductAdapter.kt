package com.example.shop

import android.graphics.Paint
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class ProductAdapter(
    private val products: List<Product>,
    private val favProductNames: List<String>,
    private val isAdmin: Boolean,
    private val onProductClick: (Product) -> Unit,
    private val onDeleteClick: (Product) -> Unit,
    private val onFavClick: (Product) -> Unit,
    private val onEditClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvName)
        val price: TextView = view.findViewById(R.id.tvPrice)
        val oldPrice: TextView = view.findViewById(R.id.tvOldPrice)
        val discountBadge: TextView = view.findViewById(R.id.tvDiscountBadge)
        val rating: TextView = view.findViewById(R.id.tvRating) // <-- ВОТ НАШ РЕЙТИНГ
        val image: ImageView = view.findViewById(R.id.productImage)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        val ivFav: ImageButton = view.findViewById(R.id.ivFav)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = products[position]

        holder.name.text = item.name

        // Скидка и Цена
        if (item.discount > 0) {
            val finalPrice = item.price - (item.price * item.discount / 100)
            holder.price.text = "${finalPrice.toInt()} ₽"
            holder.price.setTextColor(holder.itemView.context.getColor(R.color.deleteRed))

            holder.oldPrice.text = "${item.price.toInt()} ₽"
            holder.oldPrice.paintFlags = holder.oldPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.oldPrice.visibility = View.VISIBLE

            holder.discountBadge.text = "-${item.discount}%"
            holder.discountBadge.visibility = View.VISIBLE
        } else {
            holder.price.text = "${item.price.toInt()} ₽"
            holder.price.setTextColor(holder.itemView.context.getColor(R.color.textPrice))
            holder.oldPrice.visibility = View.GONE
            holder.discountBadge.visibility = View.GONE
        }

        // --- РЕЙТИНГ ---
        if (item.reviewCount > 0) {
            // Формат: "4.5 (12)"
            holder.rating.text = String.format(Locale.US, "%.1f (%d)", item.rating, item.reviewCount)
            holder.rating.visibility = View.VISIBLE
        } else {
            // Если отзывов нет - пишем "Нет отзывов" или скрываем
            holder.rating.text = "0.0 (0)"
            // holder.rating.visibility = View.INVISIBLE // Можно скрыть, если хочешь
        }

        // Картинка
        if (item.imageUri != null) {
            holder.image.setImageURI(Uri.parse(item.imageUri))
            holder.image.scaleType = ImageView.ScaleType.CENTER_CROP
            holder.image.setPadding(0,0,0,0)
        } else {
            holder.image.setImageResource(android.R.drawable.ic_menu_camera)
            holder.image.scaleType = ImageView.ScaleType.FIT_CENTER
            holder.image.setPadding(50,50,50,50)
        }

        holder.itemView.setOnClickListener { onProductClick(item) }

        // Избранное
        val isFavorite = favProductNames.contains(item.name)
        if (isFavorite) {
            holder.ivFav.setColorFilter(holder.itemView.context.getColor(R.color.colorAccent))
        } else {
            holder.ivFav.setColorFilter(holder.itemView.context.getColor(R.color.textSecondary))
        }
        holder.ivFav.setOnClickListener { onFavClick(item) }

        // Админка
        if (isAdmin) {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnDelete.setOnClickListener { onDeleteClick(item) }
            holder.itemView.setOnLongClickListener {
                onEditClick(item)
                true
            }
        } else {
            holder.btnDelete.visibility = View.GONE
            holder.itemView.setOnLongClickListener(null)
        }
    }

    override fun getItemCount(): Int = products.size
}