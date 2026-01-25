package com.example.shop

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room

class CategoriesActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: ArrayAdapter<String>
    private val categoriesList = mutableListOf<String>()
    private val categoriesObjects = mutableListOf<Category>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "shop-db")
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()

        val etNewCategory = findViewById<EditText>(R.id.etNewCategory)
        val btnAdd = findViewById<Button>(R.id.btnAddCategory)
        val listView = findViewById<ListView>(R.id.lvCategories)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, categoriesList)
        listView.adapter = adapter

        loadCategories()

        btnAdd.setOnClickListener {
            val name = etNewCategory.text.toString().trim()
            if (name.isNotEmpty()) {
                // Проверка на дубликат
                if (categoriesList.contains(name)) {
                    Toast.makeText(this, "Такая категория уже есть", Toast.LENGTH_SHORT).show()
                } else {
                    db.productDao().insertCategory(Category(name = name))
                    etNewCategory.setText("")
                    loadCategories()
                }
            }
        }

        // --- УМНОЕ УДАЛЕНИЕ ---
        listView.setOnItemClickListener { _, _, position, _ ->
            val categoryToDelete = categoriesObjects[position]

            // 1. Считаем товары в этой категории
            val count = db.productDao().getProductsCountByCategory(categoryToDelete.name)

            if (count > 0) {
                // 2. Если есть товары - СТРАШНОЕ ПРЕДУПРЕЖДЕНИЕ
                AlertDialog.Builder(this)
                    .setTitle("⚠️ Внимание!")
                    .setMessage("В категории '${categoryToDelete.name}' сейчас находится $count товаров.\n\nЕсли вы удалите категорию, эти товары останутся в базе, но перестанут отображаться в фильтрах.\n\nВы точно хотите продолжить?")
                    .setPositiveButton("Удалить всё равно") { _, _ ->
                        deleteCategory(categoryToDelete)
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            } else {
                // 3. Если товаров нет - обычное удаление
                AlertDialog.Builder(this)
                    .setTitle("Удалить категорию?")
                    .setMessage("Удалить '${categoryToDelete.name}'?")
                    .setPositiveButton("Да") { _, _ ->
                        deleteCategory(categoryToDelete)
                    }
                    .setNegativeButton("Нет", null)
                    .show()
            }
        }
    }

    private fun deleteCategory(category: Category) {
        db.productDao().deleteCategory(category)
        loadCategories()
        Toast.makeText(this, "Категория удалена", Toast.LENGTH_SHORT).show()
    }

    private fun loadCategories() {
        categoriesList.clear()
        categoriesObjects.clear()

        val list = db.productDao().getAllCategories()

        if (list.isEmpty() && db.productDao().getCategoriesCount() == 0) {
            val defaults = listOf("Электроника", "Одежда", "Еда", "Разное")
            for (name in defaults) db.productDao().insertCategory(Category(name = name))
            loadCategories()
            return
        }

        for (cat in list) {
            categoriesList.add(cat.name)
            categoriesObjects.add(cat)
        }
        adapter.notifyDataSetChanged()
    }
}