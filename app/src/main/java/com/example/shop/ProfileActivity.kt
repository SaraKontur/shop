package com.example.shop

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import androidx.appcompat.app.AppCompatDelegate // Импорт

class ProfileActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var currentUserId = 0
    private var tempAvatarUri: String? = null
    private lateinit var ivAvatar: ImageView

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                tempAvatarUri = uri.toString()
                ivAvatar.setImageURI(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        currentUserId = intent.getIntExtra("USER_ID", 0)
        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "shop-db").allowMainThreadQueries().build()

        ivAvatar = findViewById(R.id.ivProfileAvatar)
        val etName = findViewById<EditText>(R.id.etProfileName)
        val etPass = findViewById<EditText>(R.id.etProfilePass)

        // Загружаем текущие данные
        val user = db.productDao().getUserById(currentUserId)
        if (user != null) {
            etName.setText(user.displayName)
            etPass.setText(user.pass)
            if (user.avatarUri != null) {
                ivAvatar.setImageURI(Uri.parse(user.avatarUri))
                tempAvatarUri = user.avatarUri
            }
        }

        // Смена фото
        findViewById<Button>(R.id.btnChangeAvatar).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }

        // Сохранить
        findViewById<Button>(R.id.btnSaveProfile).setOnClickListener {
            if (user != null) {
                val updatedUser = user.copy(
                    displayName = etName.text.toString(),
                    pass = etPass.text.toString(),
                    avatarUri = tempAvatarUri
                )
                db.productDao().updateUser(updatedUser)
                Toast.makeText(this, "Профиль обновлен!", Toast.LENGTH_SHORT).show()
            }
        }

        // Выйти (Logout)
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            // Очищаем историю переходов, чтобы нельзя было вернуться назад кнопкой Back
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnOrders).setOnClickListener {
            val intent = Intent(this, OrdersActivity::class.java)
            intent.putExtra("USER_ID", currentUserId)
            startActivity(intent)
        }

        val btnTheme = findViewById<Button>(R.id.btnTheme)
        btnTheme.setOnClickListener {
            // Проверяем текущий режим
            val currentMode = AppCompatDelegate.getDefaultNightMode()
            if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
    }
}