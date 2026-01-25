package com.example.shop

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room

class OrderDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_detail)

        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "shop-db")
            .allowMainThreadQueries()
            .build()

        val orderId = intent.getIntExtra("ORDER_ID", 0)
        val order = db.productDao().getOrderById(orderId)

        if (order != null) {
            findViewById<TextView>(R.id.tvDetailId).text = "Заказ #${order.id}"
            findViewById<TextView>(R.id.tvDetailStatus).text = order.status
            findViewById<TextView>(R.id.tvDetailDate).text = order.date
            findViewById<TextView>(R.id.tvDetailPayment).text = "Способ оплаты: ${order.paymentMethod}"

            // Выводим список товаров
            findViewById<TextView>(R.id.tvDetailItems).text = order.itemsSummary

            findViewById<TextView>(R.id.tvDetailTotal).text = "Итого: ${order.totalPrice.toInt()} ₽"
        }

        findViewById<Button>(R.id.btnClose).setOnClickListener {
            finish()
        }
    }
}