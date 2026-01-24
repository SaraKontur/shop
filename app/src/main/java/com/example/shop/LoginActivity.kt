package com.example.shop

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "shop-db")
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()

        // --- АВТОСОЗДАНИЕ ТЕСТОВЫХ ЮЗЕРОВ ---
        if (db.productDao().checkUserExists("admin") == null) {
            db.productDao().registerUser(User(login = "admin", pass = "123456", isAdmin = true))
        }
        if (db.productDao().checkUserExists("customer") == null) {
            db.productDao().registerUser(User(login = "customer", pass = "123456", isAdmin = false))
        }
        // ------------------------------------

        val etLogin = findViewById<EditText>(R.id.etLogin)
        val etPass = findViewById<EditText>(R.id.etPass)

        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            val login = etLogin.text.toString()
            val pass = etPass.text.toString()

            val user = db.productDao().loginUser(login, pass)

            if (user != null) {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("USER_ID", user.id) // Передаем ID того, кто вошел
                intent.putExtra("IS_ADMIN", user.isAdmin)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Неверный логин или пароль", Toast.LENGTH_SHORT).show()
            }
        }

        // Переход на регистрацию
        findViewById<TextView>(R.id.tvGoToRegister).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}