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
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var rv: RecyclerView
    private var currentUserId = 0
    private var isAdmin = false

    // Для фильтров
    private var currentCategory = "Все"
    private lateinit var btnAll: Button
    private lateinit var btnPhones: Button
    private lateinit var btnClothes: Button
    private lateinit var btnFood: Button
    private lateinit var btnOther: Button

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

        // Инициализация кнопок фильтров
        btnAll = findViewById(R.id.chipAll)
        btnPhones = findViewById(R.id.chipPhones)
        btnClothes = findViewById(R.id.chipClothes)
        btnFood = findViewById(R.id.chipFood)
        btnOther = findViewById(R.id.chipOther)

        // Настраиваем клики по фильтрам
        setupFilterButton(btnAll, "Все")
        setupFilterButton(btnPhones, "Электроника")
        setupFilterButton(btnClothes, "Одежда")
        setupFilterButton(btnFood, "Еда")
        setupFilterButton(btnOther, "Разное")

        updateFilterUI("Все")

        if (isAdmin) fabAdd.visibility = View.VISIBLE else fabAdd.visibility = View.GONE
        fabAdd.setOnClickListener {
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("IS_EDIT", true)
            startActivity(intent)
        }

        // --- ЛОГИКА ОПЛАТЫ ---
        btnCheckout.setOnClickListener {
            showPaymentDialog()
        }

        findViewById<BottomNavigationView>(R.id.bottomNav).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {
                    cartPanelCard.visibility = View.GONE
                    loadProducts("Все")
                    updateFilterUI("Все")
                    true
                }
                R.id.menu_fav -> {
                    cartPanelCard.visibility = View.GONE
                    loadFavorites() // <--- ЗАГРУЖАЕМ ИЗБРАННОЕ
                    updateFilterUI("") // Сбрасываем подсветку фильтров
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

        loadProducts("Все")
    }

    override fun onResume() {
        super.onResume()
        if (findViewById<View>(R.id.cartPanelCard).visibility != View.VISIBLE) {
            loadProducts(currentCategory)
        } else {
            loadCart(rv)
        }
    }

    private fun setupFilterButton(btn: Button, category: String) {
        btn.setOnClickListener {
            currentCategory = category
            loadProducts(category)
            updateFilterUI(category)
        }
    }

    private fun updateFilterUI(selectedCategory: String) {
        val allButtons = listOf(btnAll, btnPhones, btnClothes, btnFood, btnOther)
        for (btn in allButtons) {
            if (btn.text == selectedCategory) {
                btn.setBackgroundColor(getColor(R.color.colorPrimary))
                btn.setTextColor(getColor(R.color.white))
            } else {
                btn.setBackgroundColor(Color.TRANSPARENT)
                btn.setTextColor(getColor(R.color.colorPrimary))
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
                intent.putExtra("PRICE", product.price)
                intent.putExtra("DESC", product.description)
                intent.putExtra("IMG", product.imageUri)
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
                loadProducts(category, search) // Обновляем список, чтобы сердечко перекрасилось
            },
            onEditClick = {}
        )
    }

    // --- ФУНКЦИЯ ЗАГРУЗКИ ИЗБРАННОГО ---
    private fun loadFavorites() {
        rv.layoutManager = GridLayoutManager(this, 2)
        val favItems = db.productDao().getFavItems(currentUserId)

        // Превращаем "Избранное" в полноценные товары (чтобы видеть рейтинг)
        val products = favItems.mapNotNull { fav ->
            db.productDao().getProductByName(fav.name)
        }

        // В этом списке все товары лайкнуты
        val allFavNames = products.map { it.name }

        rv.adapter = ProductAdapter(products, allFavNames, isAdmin = false,
            onProductClick = { product ->
                val intent = Intent(this, DetailActivity::class.java)
                intent.putExtra("NAME", product.name)
                intent.putExtra("PRICE", product.price)
                intent.putExtra("DESC", product.description)
                intent.putExtra("IMG", product.imageUri)
                intent.putExtra("USER_ID", currentUserId)
                startActivity(intent)
            },
            onDeleteClick = {}, // В избранном удалять товар нельзя (только лайк снять)
            onFavClick = { product ->
                db.productDao().removeFromFav(product.name, currentUserId)
                loadFavorites() // Сразу обновляем экран, товар исчезнет
            },
            onEditClick = {}
        )

        if (products.isEmpty()) {
            Toast.makeText(this, "В избранном пусто", Toast.LENGTH_SHORT).show()
        }
    }

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
    }

    private fun updateTotalPrice(items: List<CartItem>) {
        var total = 0.0
        for (item in items) { total += item.price * item.quantity }
        findViewById<TextView>(R.id.tvTotalPrice).text = "Итого: $total $"
    }

    private fun showPaymentDialog() {
        val options = arrayOf("Картой на сайте", "Картой курьеру", "Наличными курьеру")
        var selectedOption = options[0]

        AlertDialog.Builder(this)
            .setTitle("Выберите способ оплаты")
            .setSingleChoiceItems(options, 0) { _, which ->
                selectedOption = options[which]
            }
            .setPositiveButton("Оплатить") { _, _ ->
                processOrder(selectedOption)
            }
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
            summary.append("${item.name} x${item.quantity}, ")
        }

        val order = Order(
            ownerId = currentUserId,
            date = Date().toString(),
            itemsSummary = summary.toString(),
            totalPrice = total,
            status = "Создан",
            paymentMethod = paymentMethod
        )
        db.productDao().insertOrder(order)
        db.productDao().clearCart(currentUserId)

        Toast.makeText(this, "Заказ оформлен! ($paymentMethod)", Toast.LENGTH_LONG).show()
        loadCart(rv)
    }
}


/*
       !!! ДАНИЛА, ЕСЛИ ТЫ ЭТО ЧИТАЕШЬ, ЗНАЧИТ ТЕБЕ СТАЛО ИНТЕРЕСНО ЧТО МЫ ТУТ ПОНАПИСАЛИ :) !!!
 */