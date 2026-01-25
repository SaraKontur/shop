package com.example.shop

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OrdersAdapter(
    private val orders: List<Order>,
    private val isAdmin: Boolean, // Оставляем флаг, вдруг пригодится для скрытых функций
    private val onOrderClick: (Order) -> Unit // Функция клика теперь открывает ДЕТАЛИ
) : RecyclerView.Adapter<OrdersAdapter.OrderHolder>() {

    class OrderHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Используем твои старые ID из item_order.xml
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

        // ИСПРАВЛЕНО: Рубли вместо Долларов
        holder.total.text = "Итого: ${order.totalPrice.toInt()} ₽\nОплата: ${order.paymentMethod}"

        holder.status.text = order.status

        // --- ТВОЯ КРАСИВАЯ ЛОГИКА ЦВЕТОВ ---
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

        // --- ЛОГИКА КЛИКА ---
        // Теперь кликать могут ВСЕ (чтобы посмотреть детали)
        holder.itemView.setOnClickListener {
            onOrderClick(order)
        }

        // Подсказку для админа можно скрыть или изменить текст
        if (isAdmin) {
            // Можно оставить, если админ будет менять статус внутри деталей
            holder.adminHint.visibility = View.GONE
        } else {
            holder.adminHint.visibility = View.GONE
        }
    }

    override fun getItemCount() = orders.size
}