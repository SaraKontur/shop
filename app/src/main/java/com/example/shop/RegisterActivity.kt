package com.example.shop

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "shop-db")
            .allowMainThreadQueries().build()

        val etLogin = findViewById<EditText>(R.id.etRegLogin)
        val etPass = findViewById<EditText>(R.id.etRegPass)

        findViewById<Button>(R.id.btnRegisterAction).setOnClickListener {
            val login = etLogin.text.toString()
            val pass = etPass.text.toString()

            if (login.isNotEmpty() && pass.isNotEmpty()) {
                if (db.productDao().checkUserExists(login) == null) {
                    // Создаем обычного пользователя (не админа)
                    db.productDao().registerUser(User(login = login, pass = pass, isAdmin = false))
                    Toast.makeText(this, "Успешно! Теперь войдите.", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Такой логин уже занят", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}