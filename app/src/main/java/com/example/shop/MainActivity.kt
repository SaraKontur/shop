package com.example.shop

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper // <--- НУЖЕН ЭТОТ ИМПОРТ
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var rv: RecyclerView
    private var currentUserId = 0
    private var isAdmin = false

    // Для фильтров
    private var currentCategory = "Все"
    private lateinit var btnAll: Button
    private lateinit var categoriesContainer: LinearLayout

    // Переменная для свайпов (чтобы сбрасывать её при обновлении списка)
    private var itemTouchHelper: ItemTouchHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        currentUserId = intent.getIntExtra("USER_ID", 0)
        isAdmin = intent.getBooleanExtra("IS_ADMIN", false)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "shop-db")
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()

        rv = findViewById(R.id.recyclerView)
        val cartPanelCard = findViewById<MaterialCardView>(R.id.cartPanelCard)
        val btnCheckout = findViewById<Button>(R.id.btnCheckout)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAdd)

        categoriesContainer = findViewById(R.id.categoriesContainer)
        btnAll = findViewById(R.id.chipAll)

        btnAll.setOnClickListener {
            currentCategory = "Все"
            loadProducts("Все")
            updateFilterUI("Все")
        }

        if (isAdmin) fabAdd.visibility = View.VISIBLE else fabAdd.visibility = View.GONE
        fabAdd.setOnClickListener {
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("IS_EDIT", true)
            startActivity(intent)
        }

        btnCheckout.setOnClickListener { showPaymentDialog() }

        findViewById<BottomNavigationView>(R.id.bottomNav).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {
                    cartPanelCard.visibility = View.GONE
                    // При уходе с корзины отключаем свайпы, чтобы товары на главной не удалялись свайпом
                    itemTouchHelper?.attachToRecyclerView(null)
                    loadProducts("Все")
                    updateFilterUI("Все")
                    true
                }
                R.id.menu_fav -> {
                    cartPanelCard.visibility = View.GONE
                    itemTouchHelper?.attachToRecyclerView(null)
                    loadFavorites()
                    updateFilterUI("")
                    true
                }
                R.id.menu_cart -> {
                    loadCart(rv)
                    true
                }
                R.id.menu_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.putExtra("USER_ID", currentUserId)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        val searchView = findViewById<SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                loadProducts(currentCategory, newText ?: "")
                return true
            }
        })

        loadDynamicCategories()
        loadProducts("Все")
        updateFilterUI("Все")
    }

    override fun onResume() {
        super.onResume()
        loadDynamicCategories()

        if (findViewById<View>(R.id.cartPanelCard).visibility != View.VISIBLE) {
            loadProducts(currentCategory)
            updateFilterUI(currentCategory)
        } else {
            loadCart(rv)
        }
    }

    private fun loadDynamicCategories() {
        if (categoriesContainer.childCount > 1) {
            categoriesContainer.removeViews(1, categoriesContainer.childCount - 1)
        }
        val categories = db.productDao().getAllCategories()
        for (cat in categories) {
            val btn = Button(this)
            btn.text = cat.name
            btn.isAllCaps = false
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.marginEnd = 16
            btn.layoutParams = params
            btn.setOnClickListener {
                currentCategory = cat.name
                loadProducts(cat.name)
                updateFilterUI(cat.name)
            }
            categoriesContainer.addView(btn)
        }
        updateFilterUI(currentCategory)
    }

    private fun updateFilterUI(selectedCategory: String) {
        for (i in 0 until categoriesContainer.childCount) {
            val view = categoriesContainer.getChildAt(i)
            if (view is Button) {
                if (view.text == selectedCategory) {
                    view.setBackgroundColor(getColor(R.color.colorPrimary))
                    view.setTextColor(getColor(R.color.white))
                } else {
                    view.setBackgroundColor(Color.TRANSPARENT)
                    view.setTextColor(getColor(R.color.colorPrimary))
                }
            }
        }
    }

    private fun loadProducts(category: String, search: String = "") {
        rv.layoutManager = GridLayoutManager(this, 2)
        val products = db.productDao().searchProducts(search, category)
        val favs = db.productDao().getFavItems(currentUserId).map { it.name }

        rv.adapter = ProductAdapter(products, favs, isAdmin,
            onProductClick = { product ->
                val intent = Intent(this, DetailActivity::class.java)
                intent.putExtra("NAME", product.name)
                intent.putExtra("IS_EDIT", false)
                intent.putExtra("USER_ID", currentUserId)
                startActivity(intent)
            },
            onDeleteClick = { product ->
                db.productDao().deleteProduct(product)
                loadProducts(category, search)
            },
            onFavClick = { product ->
                if (favs.contains(product.name)) {
                    db.productDao().removeFromFav(product.name, currentUserId)
                } else {
                    db.productDao().addToFav(FavoriteItem(ownerId = currentUserId, name = product.name, price = product.price, imageUri = product.imageUri))
                }
                loadProducts(category, search)
            },
            onEditClick = { product ->
                val intent = Intent(this, DetailActivity::class.java)
                intent.putExtra("IS_EDIT", true)
                intent.putExtra("NAME", product.name)
                startActivity(intent)
            }
        )
    }

    private fun loadFavorites() {
        rv.layoutManager = GridLayoutManager(this, 2)
        val favItems = db.productDao().getFavItems(currentUserId)
        val products = favItems.mapNotNull { db.productDao().getProductByName(it.name) }
        val allFavNames = products.map { it.name }

        rv.adapter = ProductAdapter(products, allFavNames, isAdmin = false,
            onProductClick = { product ->
                val intent = Intent(this, DetailActivity::class.java)
                intent.putExtra("NAME", product.name)
                intent.putExtra("USER_ID", currentUserId)
                startActivity(intent)
            },
            onDeleteClick = {},
            onFavClick = { product ->
                db.productDao().removeFromFav(product.name, currentUserId)
                loadFavorites()
            },
            onEditClick = {}
        )
        if (products.isEmpty()) Toast.makeText(this, "В избранном пусто", Toast.LENGTH_SHORT).show()
    }

    // --- ОБНОВЛЕННАЯ ФУНКЦИЯ КОРЗИНЫ СО СВАЙПОМ ---
    private fun loadCart(rv: RecyclerView) {
        rv.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        val cartItems = db.productDao().getCartItems(currentUserId)
        val cartPanel = findViewById<View>(R.id.cartPanelCard)

        if (cartItems.isNotEmpty()) {
            cartPanel.visibility = View.VISIBLE
            updateTotalPrice(cartItems)
        } else {
            cartPanel.visibility = View.GONE
            Toast.makeText(this, "Корзина пуста", Toast.LENGTH_SHORT).show()
        }

        // Подключаем Адаптер
        rv.adapter = CartAdapter(cartItems,
            onDeleteClick = { item ->
                db.productDao().deleteCartItem(item)
                loadCart(rv)
            },
            onQuantityChange = { item, newQty ->
                item.quantity = newQty
                db.productDao().updateCartItem(item)
                updateTotalPrice(db.productDao().getCartItems(currentUserId))
            }
        )

        // --- ЛОГИКА СВАЙПА ---
        // 1. Удаляем старый помощник, если он был (чтобы не дублировались)
        itemTouchHelper?.attachToRecyclerView(null)

        // 2. Создаем новый помощник (Свайп ВЛЕВО или ВПРАВО)
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder): Boolean {
                return false // Перетаскивание (drag & drop) нам не нужно
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Получаем позицию элемента, который смахнули
                val position = viewHolder.adapterPosition

                // Получаем сам товар из списка
                val itemToDelete = cartItems[position]

                // Удаляем из базы
                db.productDao().deleteCartItem(itemToDelete)

                // Перезагружаем корзину (чтобы пересчиталась цена и обновился список)
                loadCart(rv)

                Toast.makeText(this@MainActivity, "${itemToDelete.name} удален", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Подключаем помощник к списку
        itemTouchHelper = ItemTouchHelper(swipeCallback)
        itemTouchHelper?.attachToRecyclerView(rv)
    }

    private fun updateTotalPrice(items: List<CartItem>) {
        var total = 0.0
        for (item in items) { total += item.price * item.quantity }
        findViewById<TextView>(R.id.tvTotalPrice).text = "Итого: ${total.toInt()} ₽"
    }

    private fun showPaymentDialog() {
        val options = arrayOf("Картой на сайте", "Картой курьеру", "Наличными курьеру")
        var selectedOption = options[0]
        AlertDialog.Builder(this)
            .setTitle("Выберите способ оплаты")
            .setSingleChoiceItems(options, 0) { _, which -> selectedOption = options[which] }
            .setPositiveButton("Оплатить") { _, _ -> processOrder(selectedOption) }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun processOrder(paymentMethod: String) {
        val cartItems = db.productDao().getCartItems(currentUserId)
        if (cartItems.isEmpty()) return

        var total = 0.0
        val summary = StringBuilder()
        for (item in cartItems) {
            total += item.price * item.quantity
            summary.append("${item.name} (x${item.quantity})\n")
        }

        val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
        val currentDate = sdf.format(Date())

        val order = Order(
            ownerId = currentUserId,
            date = currentDate,
            itemsSummary = summary.toString(),
            totalPrice = total,
            status = "Создан",
            paymentMethod = paymentMethod
        )
        db.productDao().insertOrder(order)
        db.productDao().clearCart(currentUserId)
        Toast.makeText(this, "Заказ оформлен!", Toast.LENGTH_LONG).show()
        loadCart(rv)
    }
}

/*
       !!! ДАНИЛА, ЕСЛИ ТЫ ЭТО ЧИТАЕШЬ, ЗНАЧИТ ТЕБЕ СТАЛО ИНТЕРЕСНО ЧТО МЫ ТУТ ПОНАПИСАЛИ :) !!!
 */