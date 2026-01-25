package com.example.shop

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room

class OrdersActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var rv: RecyclerView
    private var isAdmin = false
    private var currentUserId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_orders)

        currentUserId = intent.getIntExtra("USER_ID", 0)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "shop-db")
            .allowMainThreadQueries()
            .build()

        // Проверяем админа
        val user = db.productDao().getUserById(currentUserId)
        isAdmin = user?.isAdmin == true

        if (isAdmin) {
            title = "Все заказы (Админ)"
        } else {
            title = "Мои заказы"
        }

        rv = findViewById(R.id.rvOrders)
        rv.layoutManager = LinearLayoutManager(this)

        loadOrders()
    }

    override fun onResume() {
        super.onResume()
        loadOrders()
    }

    private fun loadOrders() {
        val orders = if (isAdmin) {
            db.productDao().getAllOrders()
        } else {
            db.productDao().getUserOrders(currentUserId)
        }

        // Внимание на код внутри фигурных скобок ниже:
        rv.adapter = OrdersAdapter(orders, isAdmin) { order ->

            // ИСПРАВЛЕНО: Добавили @OrdersActivity
            val intent = Intent(this@OrdersActivity, OrderDetailActivity::class.java)

            intent.putExtra("ORDER_ID", order.id)
            startActivity(intent)
        }

        if (orders.isEmpty()) {
            Toast.makeText(this, "Список заказов пуст", Toast.LENGTH_SHORT).show()
        }
    }
}