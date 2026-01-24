package com.example.shop

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var isAdmin = false
    private var currentUserId = 0
    private var currentSearchText = ""
    private var currentCategory = "Все"
    private var tempImageUri: String? = null
    private lateinit var photoPreview: ImageView

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                tempImageUri = uri.toString()
                photoPreview.setImageURI(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        currentUserId = intent.getIntExtra("USER_ID", 0)
        isAdmin = intent.getBooleanExtra("IS_ADMIN", false)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "shop-db")
            .allowMainThreadQueries().fallbackToDestructiveMigration().build()

        // 1. Создаем переменную rv ОДИН раз
        val rv = findViewById<RecyclerView>(R.id.recyclerView)
        val topPanel = findViewById<LinearLayout>(R.id.topPanel)
        val cartPanel = findViewById<LinearLayout>(R.id.cartPanel)
        val searchView = findViewById<SearchView>(R.id.searchView)
        val fab = findViewById<FloatingActionButton>(R.id.fabAdd)

        loadProducts(rv)

        // 2. УДАЛИЛИ ПОВТОРНУЮ СТРОКУ "val rv = ..."
        // Просто используем уже созданную rv
        setupSwipeToDelete(rv)

        // Поиск
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchText = newText ?: ""
                loadProducts(rv)
                return true
            }
        })

        // Чипсы
        setupCategoryButton(findViewById(R.id.chipAll), "Все", rv)
        setupCategoryButton(findViewById(R.id.chipPhones), "Телефоны", rv)
        setupCategoryButton(findViewById(R.id.chipClothes), "Одежда", rv)
        setupCategoryButton(findViewById(R.id.chipFood), "Еда", rv)
        setupCategoryButton(findViewById(R.id.chipOther), "Разное", rv)

        // Кнопка добавления
        if (isAdmin) {
            fab.visibility = View.VISIBLE
            fab.setOnClickListener { showProductDialog(rv, null) }
        } else {
            fab.visibility = View.GONE
        }

        // Кнопка "Купить всё"
        findViewById<Button>(R.id.btnCheckout).setOnClickListener {
            val cartItems = db.productDao().getCartItems(currentUserId)
            if (cartItems.isNotEmpty()) {
                val summary = cartItems.joinToString { "${it.name} (${it.quantity} шт)" }
                val total = cartItems.sumOf { it.price * it.quantity }

                val order = Order(
                    ownerId = currentUserId,
                    date = Date().toString(),
                    itemsSummary = summary,
                    totalPrice = total
                )
                db.productDao().insertOrder(order)
                db.productDao().clearCart(currentUserId)

                Toast.makeText(this, "Заказ оформлен!", Toast.LENGTH_LONG).show()
                loadCart(rv)
            }
        }

        // Нижнее меню
        findViewById<BottomNavigationView>(R.id.bottomNav).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    topPanel.visibility = View.VISIBLE
                    cartPanel.visibility = View.GONE
                    fab.visibility = if (isAdmin) View.VISIBLE else View.GONE
                    loadProducts(rv)
                    true
                }
                R.id.nav_cart -> {
                    topPanel.visibility = View.GONE
                    cartPanel.visibility = View.VISIBLE
                    fab.visibility = View.GONE
                    loadCart(rv)
                    true
                }
                R.id.nav_fav -> {
                    topPanel.visibility = View.GONE
                    cartPanel.visibility = View.GONE
                    fab.visibility = View.GONE
                    loadFavorites(rv)
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.putExtra("USER_ID", currentUserId)
                    startActivity(intent)
                    false
                }
                else -> false
            }
        }
    }

    private fun setupCategoryButton(btn: Button, category: String, rv: RecyclerView) {
        btn.setOnClickListener {
            currentCategory = category
            loadProducts(rv)
        }
    }

    // --- 1. ТОВАРЫ ---
    private fun loadProducts(rv: RecyclerView) {
        rv.layoutManager = GridLayoutManager(this, 2)

        // 1. Получаем товары
        val products = db.productDao().searchProducts(currentSearchText, currentCategory)

        // 2. Получаем список имен лайкнутых товаров для текущего юзера
        val favItems = db.productDao().getFavItems(currentUserId)
        val favNames = favItems.map { it.name } // Превращаем List<FavoriteItem> в List<String>

        rv.adapter = ProductAdapter(products, favNames, isAdmin, // <-- Передаем favNames
            onProductClick = { product ->
                val intent = Intent(this, DetailActivity::class.java)
                intent.putExtra("NAME", product.name)
                intent.putExtra("PRICE", product.price)
                intent.putExtra("DESC", product.description)
                intent.putExtra("IMG", product.imageUri)
                intent.putExtra("USER_ID", currentUserId)
                startActivity(intent)
            },
            onDeleteClick = { product ->
                db.productDao().deleteProduct(product)
                loadProducts(rv)
            },
            onFavClick = { product ->
                // Логика лайка/дизлайка
                if (!db.productDao().isFavorite(product.name, currentUserId)) {
                    val fav = FavoriteItem(ownerId = currentUserId, name = product.name, price = product.price, imageUri = product.imageUri)
                    db.productDao().addToFav(fav)
                } else {
                    db.productDao().removeFromFav(product.name, currentUserId)
                }
                // ВАЖНО: Перезагружаем список, чтобы сердечко перекрасилось
                loadProducts(rv)
            },
            onEditClick = { product ->
                showProductDialog(rv, product)
            }
        )
    }

    private fun loadCart(rv: RecyclerView) {
        rv.layoutManager = LinearLayoutManager(this)
        val cartItems = db.productDao().getCartItems(currentUserId)

        // Считаем сумму
        val totalSum = cartItems.sumOf { it.price * it.quantity }
        findViewById<TextView>(R.id.tvTotalPrice).text = "Итого: $totalSum $"

        rv.adapter = CartAdapter(cartItems,
            onPlusClick = { item ->
                item.quantity += 1
                db.productDao().updateCartItem(item)
                loadCart(rv)
            },
            onMinusClick = { item ->
                if (item.quantity > 1) {
                    item.quantity -= 1
                    db.productDao().updateCartItem(item)
                } else {
                    db.productDao().deleteCartItem(item)
                }
                loadCart(rv)
            }
        )
    }

    // --- 3. ИЗБРАННОЕ ---
    private fun loadFavorites(rv: RecyclerView) {
        rv.layoutManager = GridLayoutManager(this, 2)
        val favItems = db.productDao().getFavItems(currentUserId)

        // МАГИЯ: Превращаем FavoriteItem в настоящий Product с рейтингом
        val products = favItems.mapNotNull { fav ->
            // Ищем актуальные данные о товаре в главной таблице
            db.productDao().getProductByName(fav.name)
        }

        // В "Избранном" все товары лайкнуты
        val allNames = products.map { it.name }

        rv.adapter = ProductAdapter(products, allNames, isAdmin = false,
            onProductClick = { product ->
                val intent = Intent(this, DetailActivity::class.java)
                intent.putExtra("NAME", product.name)
                intent.putExtra("PRICE", product.price)
                intent.putExtra("DESC", product.description)
                intent.putExtra("IMG", product.imageUri)
                intent.putExtra("USER_ID", currentUserId)
                startActivity(intent)
            },
            onDeleteClick = {},
            onFavClick = { product ->
                db.productDao().removeFromFav(product.name, currentUserId)
                loadFavorites(rv)
            },
            onEditClick = {}
        )

        if (products.isEmpty()) Toast.makeText(this, "Тут пусто", Toast.LENGTH_SHORT).show()
    }

    private fun setupSwipeToDelete(rv: RecyclerView) {
        val callback = object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0, androidx.recyclerview.widget.ItemTouchHelper.LEFT) {
            override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder): Boolean {
                return false // Перетаскивание (drag & drop) нам пока не нужно
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Узнаем позицию элемента, который смахнули
                val position = viewHolder.adapterPosition

                // Тут нужно понять, какой список сейчас отображается (Товары или Корзина?)
                // Это сложный момент, так как адаптер у нас один на всех.
                // ДЛЯ ПРОСТОТЫ: Сделаем свайп ТОЛЬКО ДЛЯ КОРЗИНЫ.

                // Проверяем, включена ли сейчас панель корзины (это значит мы во вкладке Корзина)
                val cartPanel = findViewById<LinearLayout>(R.id.cartPanel)
                if (cartPanel.visibility == View.VISIBLE) {
                    // Мы в корзине
                    val cartItems = db.productDao().getCartItems(currentUserId)
                    val itemToDelete = cartItems[position]

                    db.productDao().deleteCartItem(itemToDelete)
                    loadCart(rv) // Обновляем экран
                    Toast.makeText(this@MainActivity, "Удалено", Toast.LENGTH_SHORT).show()
                } else {
                    // Мы в списке товаров (Админ может удалять?)
                    // Пока лучше запретить свайп в списке товаров, чтобы случайно не удалить весь товар из базы
                    loadProducts(rv) // Просто возвращаем элемент на место (отменяем свайп)
                    Toast.makeText(this@MainActivity, "Свайп работает только в корзине", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val itemTouchHelper = androidx.recyclerview.widget.ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(rv)
    }


    // УНИВЕРСАЛЬНЫЙ ДИАЛОГ (И для создания, и для редактирования)
    private fun showProductDialog(rv: RecyclerView, productToEdit: Product?) {
        val dialogView = LinearLayout(this)
        dialogView.orientation = LinearLayout.VERTICAL
        dialogView.setPadding(50, 50, 50, 50)

        val nameInput = EditText(this)
        nameInput.hint = "Название"
        dialogView.addView(nameInput)

        val priceInput = EditText(this)
        priceInput.hint = "Цена"
        dialogView.addView(priceInput)

        val descInput = EditText(this)
        descInput.hint = "Описание"
        dialogView.addView(descInput)

        val spinner = Spinner(this)
        val categories = arrayOf("Телефоны", "Одежда", "Еда", "Разное")
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        dialogView.addView(spinner)

        val btnPhoto = Button(this)
        btnPhoto.text = "Загрузить фото"
        dialogView.addView(btnPhoto)

        photoPreview = ImageView(this)
        photoPreview.layoutParams = LinearLayout.LayoutParams(200, 200)
        dialogView.addView(photoPreview)

        // --- ЕСЛИ ЭТО РЕДАКТИРОВАНИЕ, ЗАПОЛНЯЕМ ПОЛЯ ---
        if (productToEdit != null) {
            nameInput.setText(productToEdit.name)
            priceInput.setText(productToEdit.price.toString())
            descInput.setText(productToEdit.description)
            // Картинку восстанавливаем
            tempImageUri = productToEdit.imageUri
            if (tempImageUri != null) photoPreview.setImageURI(Uri.parse(tempImageUri))

            // Категорию в спиннере выбрать сложновато в одну строку, оставим по умолчанию пока
        } else {
            tempImageUri = null // Очищаем для нового
        }
        // ------------------------------------------------

        btnPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }

        AlertDialog.Builder(this)
            .setTitle(if (productToEdit == null) "Новый товар" else "Редактировать")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val name = nameInput.text.toString()
                val price = priceInput.text.toString().toDoubleOrNull() ?: 0.0
                val desc = descInput.text.toString()
                val category = spinner.selectedItem.toString()

                if (productToEdit == null) {
                    // СОЗДАНИЕ
                    val newProduct = Product(name = name, price = price, description = desc, imageUri = tempImageUri, category = category)
                    db.productDao().insertProduct(newProduct)
                } else {
                    // ОБНОВЛЕНИЕ (копируем старый ID!)
                    val updatedProduct = productToEdit.copy(name = name, price = price, description = desc, imageUri = tempImageUri, category = category)
                    db.productDao().updateProduct(updatedProduct)
                }
                loadProducts(rv)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}


/*
    Данила, если ты это читаешь, значит ты молодец и решил посмотреть что мы тут понаписали :)
 */