package com.example.shop

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OrdersAdapter(
    private val orders: List<Order>,
    private val isAdmin: Boolean,
    private val onOrderClick: (Order) -> Unit // Действие при клике
) : RecyclerView.Adapter<OrdersAdapter.OrderHolder>() {

    class OrderHolder(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.orderDate)
        val status: TextView = view.findViewById(R.id.orderStatus)
        val items: TextView = view.findViewById(R.id.orderItems)
        val total: TextView = view.findViewById(R.id.orderTotal)
        val adminHint: TextView = view.findViewById(R.id.tvAdminHint)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return OrderHolder(view)
    }

    override fun onBindViewHolder(holder: OrderHolder, position: Int) {
        val order = orders[position]

        holder.date.text = order.date
        holder.items.text = order.itemsSummary
        holder.total.text = "Итого: ${order.totalPrice} $\nОплата: ${order.paymentMethod}"
        holder.status.text = order.status

        // Раскрашиваем статусы
        when (order.status) {
            "Создан" -> {
                holder.status.setTextColor(Color.parseColor("#2196F3")) // Синий
                holder.status.setBackgroundColor(Color.parseColor("#E3F2FD"))
            }
            "Сборка" -> {
                holder.status.setTextColor(Color.parseColor("#FF9800")) // Оранжевый
                holder.status.setBackgroundColor(Color.parseColor("#FFF3E0"))
            }
            "Доставка" -> {
                holder.status.setTextColor(Color.parseColor("#9C27B0")) // Фиолетовый
                holder.status.setBackgroundColor(Color.parseColor("#F3E5F5"))
            }
            "Доставлен" -> {
                holder.status.setTextColor(Color.parseColor("#4CAF50")) // Зеленый
                holder.status.setBackgroundColor(Color.parseColor("#E8F5E9"))
            }
        }

        // Если Админ — показываем подсказку и включаем клик
        if (isAdmin) {
            holder.adminHint.visibility = View.VISIBLE
            holder.itemView.setOnClickListener { onOrderClick(order) }
        } else {
            holder.adminHint.visibility = View.GONE
            holder.itemView.setOnClickListener(null) // Обычный юзер не может менять статус
        }
    }

    override fun getItemCount() = orders.size
}