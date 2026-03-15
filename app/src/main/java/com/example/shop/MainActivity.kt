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
import androidx.recyclerview.widget.ItemTouchHelper
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

        prePopulateDatabase()

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

        itemTouchHelper?.attachToRecyclerView(null)

        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val itemToDelete = cartItems[position]
                db.productDao().deleteCartItem(itemToDelete)
                loadCart(rv)
                Toast.makeText(this@MainActivity, "${itemToDelete.name} удален", Toast.LENGTH_SHORT).show()
            }
        }

        itemTouchHelper = ItemTouchHelper(swipeCallback)
        itemTouchHelper?.attachToRecyclerView(rv)
    }

    private fun updateTotalPrice(items: List<CartItem>) {
        var total = 0.0
        for (item in items) { total += item.price * item.quantity }
        findViewById<TextView>(R.id.tvTotalPrice).text = "Итого: ${total.toInt()} ₽"
    }

    // --- ЛОГИКА ОФОРМЛЕНИЯ ЗАКАЗА ---

    private fun showPaymentDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_checkout, null)

        val rgClientType = view.findViewById<android.widget.RadioGroup>(R.id.rgClientType)
        val llJurFields = view.findViewById<android.widget.LinearLayout>(R.id.llJurFields)
        val etInn = view.findViewById<android.widget.EditText>(R.id.etInn)
        val etCompany = view.findViewById<android.widget.EditText>(R.id.etCompany)

        val rbCardSite = view.findViewById<android.widget.RadioButton>(R.id.rbCardSite)
        val rbCardCourier = view.findViewById<android.widget.RadioButton>(R.id.rbCardCourier)
        val rbCash = view.findViewById<android.widget.RadioButton>(R.id.rbCash)
        val rbAccount = view.findViewById<android.widget.RadioButton>(R.id.rbAccount)

        // Логика переключения Физ/Юр лицо
        rgClientType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbJur) {
                // Включили Юр. лицо
                llJurFields.visibility = View.VISIBLE
                rbCardSite.visibility = View.GONE
                rbCardCourier.visibility = View.GONE
                rbCash.visibility = View.GONE
                rbAccount.visibility = View.VISIBLE
                rbAccount.isChecked = true
            } else {
                // Включили Физ. лицо
                llJurFields.visibility = View.GONE
                rbCardSite.visibility = View.VISIBLE
                rbCardCourier.visibility = View.VISIBLE
                rbCash.visibility = View.VISIBLE
                rbAccount.visibility = View.GONE
                rbCardSite.isChecked = true
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Оформление заказа")
            .setView(view)
            .setPositiveButton("Оформить") { _, _ ->
                val isB2B = rgClientType.checkedRadioButtonId == R.id.rbJur
                var finalPaymentMethod = ""

                if (isB2B) {
                    val inn = etInn.text.toString().trim()
                    val company = etCompany.text.toString().trim()
                    if (inn.isEmpty() || company.isEmpty()) {
                        Toast.makeText(this, "Заполните ИНН и Название!", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    finalPaymentMethod = "Счет на оплату ($company, ИНН: $inn)"
                    processOrder(finalPaymentMethod, isB2B, company, inn)
                } else {
                    finalPaymentMethod = when {
                        rbCardCourier.isChecked -> "Картой курьеру"
                        rbCash.isChecked -> "Наличными курьеру"
                        else -> "Картой на сайте"
                    }
                    processOrder(finalPaymentMethod, isB2B, "", "")
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun processOrder(paymentMethod: String, isB2B: Boolean, company: String, inn: String) {
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
        loadCart(rv)

        // Если это Юр. лицо, показываем сгенерированный "Счет"
        if (isB2B) {
            showInvoiceDialog(company, inn, total, currentDate)
        } else {
            Toast.makeText(this, "Заказ оформлен! ($paymentMethod)", Toast.LENGTH_LONG).show()
        }
    }

    // Окно генерации счета для бухгалтерии
    private fun showInvoiceDialog(company: String, inn: String, total: Double, date: String) {
        val invoiceText = """
            Поставщик: ООО "Мой Магазин" (ИНН: 7701234567)
            Покупатель: $company (ИНН: $inn)
            Дата: $date
            
            К оплате: ${total.toInt()} ₽
            Без НДС.
            
            Реквизиты для оплаты:
            Р/С: 40702810123450000001
            БИК: 044525225
            
            Статус заказа изменится на "Сборка" после поступления средств.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Счет на оплату сформирован")
            .setMessage(invoiceText)
            .setPositiveButton("Скачать PDF") { _, _ ->
                downloadPdf(invoiceText, company)
            }
            .setNeutralButton("Закрыть", null)
            .setCancelable(false)
            .show()
    }

    // --- НОВАЯ ФУНКЦИЯ: РЕАЛЬНОЕ СОЗДАНИЕ И СОХРАНЕНИЕ PDF ---
    private fun downloadPdf(text: String, companyName: String) {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        val paint = android.graphics.Paint()
        paint.textSize = 16f

        var y = 50f
        for (line in text.split("\n")) {
            page.canvas.drawText(line, 50f, y, paint)
            y += 25f
        }
        pdfDocument.finishPage(page)

        val safeCompanyName = companyName.replace(" ", "_")
        val fileName = "Invoice_${safeCompanyName}_${System.currentTimeMillis()}.pdf"

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val resolver = contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { pdfDocument.writeTo(it) }
                    Toast.makeText(this, "✅ PDF сохранен в Загрузки!", Toast.LENGTH_LONG).show()
                }
            } else {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val file = java.io.File(downloadsDir, fileName)
                pdfDocument.writeTo(java.io.FileOutputStream(file))
                Toast.makeText(this, "✅ PDF сохранен в Загрузки!", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "❌ Ошибка сохранения: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            pdfDocument.close()
        }
    }

    private fun prePopulateDatabase() {
        // Проверяем, есть ли уже товары в базе
        val currentProducts = db.productDao().getAllProducts()

        // Если база пустая (первый запуск приложения)
        if (currentProducts.isEmpty()) {

            // 1. Создаем аптечные категории
            if (db.productDao().getCategoriesCount() == 0) {
                db.productDao().insertCategory(Category(name = "Обезболивающие"))
                db.productDao().insertCategory(Category(name = "От простуды"))
                db.productDao().insertCategory(Category(name = "Витамины"))
            }

            // 2. Создаем стартовые лекарства
            val p1 = Product(
                name = "Нурофен Экспресс",
                price = 350.0,
                discount = 0,
                description = "Капсулы с жидким центром. Быстрое и эффективное средство от головной и мышечной боли.",
                imageUri = "res/drawable/product2.jpg",
                category = "Обезболивающие"
            )

            val p2 = Product(
                name = "ТераФлю Экстра",
                price = 480.0,
                discount = 10, // Скидка 10%
                description = "Порошок со вкусом лимона. Снимает жар, заложенность носа и боль в горле.",
                imageUri = "res/drawable/product1.jpeg",
                category = "От простуды"
            )

            val p3 = Product(
                name = "Витамин D3 (Аквадетрим)",
                price = 250.0,
                discount = 5, // Скидка 5%
                description = "Водный раствор витамина D3. Необходим для укрепления иммунитета и костной ткани.",
                imageUri = "res/drawable/product3.jpg",
                category = "Витамины"
            )

            // 3. Сохраняем лекарства в базу данных
            db.productDao().insertProduct(p1)
            db.productDao().insertProduct(p2)
            db.productDao().insertProduct(p3)
        }
    }
}

/*
       !!! ДАНИЛА, ЕСЛИ ТЫ ЭТО ЧИТАЕШЬ, ЗНАЧИТ ТЕБЕ СТАЛО ИНТЕРЕСНО ЧТО МЫ ТУТ ПОНАПИСАЛИ :) !!!
 */