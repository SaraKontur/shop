package com.example.shop

import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton // <--- ВОТ ЭТОГО НЕ ХВАТАЛО
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

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
        val image: ImageView = view.findViewById(R.id.productImage)

        // Теперь Kotlin знает, что такое ImageButton
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        val ivFav: ImageButton = view.findViewById(R.id.ivFav)

        val rating: TextView = view.findViewById(R.id.tvRating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = products[position]

        holder.name.text = item.name
        holder.price.text = "${item.price} $"

        if (item.imageUri != null) {
            holder.image.setImageURI(Uri.parse(item.imageUri))
        } else {
            holder.image.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        // Логика рейтинга с количеством
        if (item.rating > 0) {
            holder.rating.text = String.format("%.1f (%d)", item.rating, item.reviewCount)
            holder.rating.visibility = View.VISIBLE
        } else {
            holder.rating.text = "New"
            holder.rating.visibility = View.VISIBLE
        }

        holder.itemView.setOnClickListener { onProductClick(item) }

        // --- ЛОГИКА СЕРДЕЧКА ---
        val isFavorite = favProductNames.contains(item.name)

        if (isFavorite) {
            // Лайкнуто - Акцентный цвет
            holder.ivFav.setColorFilter(holder.itemView.context.getColor(R.color.colorAccent))
        } else {
            // Не лайкнуто - Серый цвет
            holder.ivFav.setColorFilter(holder.itemView.context.getColor(R.color.textSecondary))
        }

        holder.ivFav.setOnClickListener {
            onFavClick(item)
        }

        // --- ЛОГИКА АДМИНА ---
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