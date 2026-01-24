package com.example.shop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room

class OrdersActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_orders)

        val userId = intent.getIntExtra("USER_ID", 0)
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "shop-db").allowMainThreadQueries().build()
        val orders = db.productDao().getUserOrders(userId)

        val rv = findViewById<RecyclerView>(R.id.rvOrders)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = object : RecyclerView.Adapter<OrderHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderHolder {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
                return OrderHolder(view)
            }
            override fun onBindViewHolder(holder: OrderHolder, position: Int) {
                val order = orders[position]
                holder.date.text = "Дата: ${order.date}"
                holder.items.text = order.itemsSummary
                holder.total.text = "Итого: ${order.totalPrice} $"
            }
            override fun getItemCount() = orders.size
        }
    }
    class OrderHolder(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.orderDate)
        val items: TextView = view.findViewById(R.id.orderItems)
        val total: TextView = view.findViewById(R.id.orderTotal)
    }
}