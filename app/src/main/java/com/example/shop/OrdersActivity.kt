package com.example.shop

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

        // ВАЖНО: Нам нужно знать, админ это или нет.
        // Давай проверим это через базу данных по ID
        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "shop-db").allowMainThreadQueries().build()
        val user = db.productDao().getUserById(currentUserId)
        isAdmin = user?.isAdmin == true

        // Обновим заголовок, если админ
        if (isAdmin) {
            // Найдем TextView заголовка, если он есть, или просто оставим как есть
            // Можно программно поменять Title окна
            title = "Управление заказами (Админ)"
        }

        rv = findViewById(R.id.rvOrders)
        rv.layoutManager = LinearLayoutManager(this)

        loadOrders()
    }

    private fun loadOrders() {
        // Логика выборки
        val orders = if (isAdmin) {
            db.productDao().getAllOrders() // Админ видит всё
        } else {
            db.productDao().getUserOrders(currentUserId) // Юзер только свое
        }

        rv.adapter = OrdersAdapter(orders, isAdmin) { order ->
            // Этот код сработает ТОЛЬКО если нажмет Админ (см. Adapter)
            showStatusDialog(order)
        }

        if (orders.isEmpty()) {
            Toast.makeText(this, "Список заказов пуст", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showStatusDialog(order: Order) {
        val statuses = arrayOf("Создан", "Сборка", "Доставка", "Доставлен")

        AlertDialog.Builder(this)
            .setTitle("Изменить статус заказа №${order.id}")
            .setItems(statuses) { _, which ->
                val newStatus = statuses[which]

                // Создаем копию заказа с новым статусом
                val updatedOrder = order.copy(status = newStatus)

                // Сохраняем в БД
                db.productDao().updateOrder(updatedOrder)

                Toast.makeText(this, "Статус обновлен: $newStatus", Toast.LENGTH_SHORT).show()
                loadOrders() // Обновляем список на экране
            }
            .show()
    }
}