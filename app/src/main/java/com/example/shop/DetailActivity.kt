package com.example.shop

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetailActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var productId = 0
    private var isEditMode = false
    private var productMediaUri: String? = null

    private var reviewMediaUri: String? = null
    private var reviewMediaType: String? = null
    private lateinit var ivReviewPreview: ImageView

    private val pickProductImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                productMediaUri = uri.toString()

                val iv = findViewById<ImageView>(R.id.detailImage)
                iv.setImageURI(uri)
                iv.scaleType = ImageView.ScaleType.CENTER_CROP
                iv.setPadding(0,0,0,0)
            }
        }
    }

    private val pickReviewMediaLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                reviewMediaUri = uri.toString()
                val type = contentResolver.getType(uri) ?: ""
                reviewMediaType = if (type.contains("video")) "video" else "image"
                ivReviewPreview.visibility = View.VISIBLE
                ivReviewPreview.setImageURI(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "shop-db")
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()

        isEditMode = intent.getBooleanExtra("IS_EDIT", false)
        val nameFromIntent = intent.getStringExtra("NAME")

        // Пытаемся найти товар, если передано имя
        var productToEdit: Product? = null
        if (nameFromIntent != null) {
            productToEdit = db.productDao().getProductByName(nameFromIntent)
            productId = productToEdit?.id ?: 0
            // Если мы редактируем, запоминаем старое фото
            if (isEditMode) productMediaUri = productToEdit?.imageUri
        }

        val currentUserId = intent.getIntExtra("USER_ID", 0)

        val ivImage = findViewById<ImageView>(R.id.detailImage)
        val btnUpload = findViewById<Button>(R.id.btnUploadImage)

        val tvName = findViewById<TextView>(R.id.detailName)
        val tvPrice = findViewById<TextView>(R.id.detailPrice)
        val tvDesc = findViewById<TextView>(R.id.detailDesc)

        val etName = findViewById<EditText>(R.id.etName)
        val etPrice = findViewById<EditText>(R.id.etPrice)
        val etDiscount = findViewById<EditText>(R.id.etDiscount) // СКИДКА
        val etDesc = findViewById<EditText>(R.id.etDesc)

        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)

        // ЗАГРУЖАЕМ КАТЕГОРИИ ИЗ БАЗЫ
        val dbCategories = db.productDao().getAllCategories()
        val categoryNames = mutableListOf<String>()

        // Если пусто (первый запуск), добавляем стандартные
        if (dbCategories.isEmpty()) {
            val defaults = listOf("Электроника", "Одежда", "Еда", "Разное")
            for (name in defaults) db.productDao().insertCategory(Category(name = name))
            categoryNames.addAll(defaults)
        } else {
            for (cat in dbCategories) categoryNames.add(cat.name)
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter


        val btnCart = findViewById<Button>(R.id.btnAddToCart)
        val btnSave = findViewById<Button>(R.id.btnSaveProduct)
        val reviewsBlock = findViewById<LinearLayout>(R.id.reviewsBlock)

        if (isEditMode) {
            // --- РЕЖИМ РЕДАКТОРА / СОЗДАНИЯ ---
            etName.visibility = View.VISIBLE
            etPrice.visibility = View.VISIBLE
            etDiscount.visibility = View.VISIBLE // Показываем поле скидки
            etDesc.visibility = View.VISIBLE
            spinnerCategory.visibility = View.VISIBLE
            btnUpload.visibility = View.VISIBLE
            btnSave.visibility = View.VISIBLE

            tvName.visibility = View.GONE
            tvPrice.visibility = View.GONE
            tvDesc.visibility = View.GONE
            btnCart.visibility = View.GONE
            reviewsBlock.visibility = View.GONE

            // Если это РЕДАКТИРОВАНИЕ (товар уже был), заполняем поля
            if (productToEdit != null) {
                etName.setText(productToEdit.name)
                etPrice.setText(productToEdit.price.toString())
                etDiscount.setText(productToEdit.discount.toString())
                etDesc.setText(productToEdit.description)

                // ИСПРАВЛЕНО: используем categoryNames вместо categories
                val catIndex = categoryNames.indexOf(productToEdit.category)
                if (catIndex >= 0) spinnerCategory.setSelection(catIndex)

                // Грузим фото
                if (productToEdit.imageUri != null) {
                    ivImage.setImageURI(Uri.parse(productToEdit.imageUri))
                    ivImage.scaleType = ImageView.ScaleType.CENTER_CROP
                    ivImage.setPadding(0,0,0,0)
                }
            }

            btnUpload.setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                }
                pickProductImageLauncher.launch(intent)
            }

            btnSave.setOnClickListener {
                val name = etName.text.toString()
                val priceStr = etPrice.text.toString()
                val discountStr = etDiscount.text.toString()
                val desc = etDesc.text.toString()
                val cat = spinnerCategory.selectedItem.toString()

                if (name.isNotEmpty() && priceStr.isNotEmpty()) {
                    val price = priceStr.toDoubleOrNull() ?: 0.0
                    val discount = discountStr.toIntOrNull() ?: 0

                    if (productId == 0) {
                        // ЭТО СОЗДАНИЕ НОВОГО
                        val newProduct = Product(
                            name = name,
                            price = price,
                            discount = discount,
                            description = desc,
                            category = cat,
                            imageUri = productMediaUri,
                            rating = 0f,
                            reviewCount = 0
                        )
                        db.productDao().insertProduct(newProduct)
                        Toast.makeText(this, "Товар создан!", Toast.LENGTH_SHORT).show()
                    } else {
                        // ЭТО ОБНОВЛЕНИЕ СТАРОГО
                        val updatedProduct = Product(
                            id = productId,
                            name = name,
                            price = price,
                            discount = discount,
                            description = desc,
                            category = cat,
                            imageUri = productMediaUri,
                            rating = productToEdit?.rating ?: 0f,
                            reviewCount = productToEdit?.reviewCount ?: 0
                        )
                        db.productDao().updateProduct(updatedProduct)
                        Toast.makeText(this, "Товар обновлен!", Toast.LENGTH_SHORT).show()
                    }

                    finish()
                } else {
                    Toast.makeText(this, "Заполните название и цену", Toast.LENGTH_SHORT).show()
                }
            }

        } else {
            // --- РЕЖИМ ПРОСМОТРА ---
            if (productToEdit != null) {
                tvName.text = productToEdit.name

                // Расчет цены для просмотра
                val finalPrice = productToEdit.price - (productToEdit.price * productToEdit.discount / 100)
                tvPrice.text = "${finalPrice.toInt()} ₽"
                if (productToEdit.discount > 0) {
                    tvPrice.append(" (Скидка ${productToEdit.discount}%)")
                }

                tvDesc.text = productToEdit.description

                if (productToEdit.imageUri != null) {
                    ivImage.setImageURI(Uri.parse(productToEdit.imageUri))
                    ivImage.scaleType = ImageView.ScaleType.CENTER_CROP
                    ivImage.setPadding(0,0,0,0)
                }
            }

            btnCart.setOnClickListener {
                if (nameFromIntent != null) {
                    val price = productToEdit?.price ?: 0.0
                    // В корзину кладем цену СО СКИДКОЙ
                    val finalPrice = price - (price * (productToEdit?.discount ?: 0) / 100)

                    val existingItem = db.productDao().getCartItemByName(nameFromIntent, currentUserId)
                    if (existingItem != null) {
                        existingItem.quantity += 1
                        db.productDao().updateCartItem(existingItem)
                    } else {
                        val cartItem = CartItem(
                            ownerId = currentUserId,
                            name = nameFromIntent,
                            price = finalPrice, // <-- Цена со скидкой
                            imageUri = productToEdit?.imageUri,
                            quantity = 1
                        )
                        db.productDao().addToCart(cartItem)
                    }
                    Toast.makeText(this, "В корзине!", Toast.LENGTH_SHORT).show()
                }
            }

            ivReviewPreview = findViewById(R.id.ivReviewPreview)
            findViewById<Button>(R.id.btnAttachMedia).setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
                }
                pickReviewMediaLauncher.launch(intent)
            }

            findViewById<Button>(R.id.btnSendReview).setOnClickListener {
                val rating = findViewById<RatingBar>(R.id.ratingBar).rating.toInt()
                val etReview = findViewById<EditText>(R.id.etReview)
                val text = etReview.text.toString()

                if (rating > 0) {
                    val user = db.productDao().getUserById(currentUserId)
                    val userName = user?.displayName ?: user?.login ?: "Аноним"

                    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    val currentDate = sdf.format(Date())

                    val review = Review(productId = productId, userId = currentUserId, userName = userName, rating = rating, text = text, date = currentDate, mediaUri = reviewMediaUri, mediaType = reviewMediaType)
                    db.productDao().addReview(review)

                    if (productToEdit != null) {
                        val newAvg = db.productDao().getAverageRating(productId)
                        val newCount = db.productDao().getReviewCount(productId)
                        db.productDao().updateProduct(productToEdit.copy(rating = newAvg, reviewCount = newCount))
                    }

                    Toast.makeText(this, "Отзыв отправлен", Toast.LENGTH_SHORT).show()
                    etReview.setText("")
                    findViewById<RatingBar>(R.id.ratingBar).rating = 0f
                    ivReviewPreview.visibility = View.GONE
                    reviewMediaUri = null
                    loadReviews()
                }
            }
            loadReviews()
        }
    }

    private fun loadReviews() {
        val container = findViewById<LinearLayout>(R.id.reviewsContainer)
        container.removeAllViews()
        val reviews = db.productDao().getReviewsForProduct(productId)

        for (review in reviews) {
            val view = LayoutInflater.from(this).inflate(R.layout.item_review, container, false)

            val tvAuthor = view.findViewById<TextView>(R.id.reviewAuthor)
            val tvDate = view.findViewById<TextView>(R.id.reviewDate)
            val tvText = view.findViewById<TextView>(R.id.reviewText)
            val ratingBar = view.findViewById<RatingBar>(R.id.reviewRating)
            val ivReviewImage = view.findViewById<ImageView>(R.id.reviewImage)

            tvAuthor.text = review.userName
            tvDate.text = review.date
            tvText.text = review.text
            ratingBar.rating = review.rating.toFloat()

            if (review.mediaUri != null && review.mediaUri.isNotEmpty()) {
                ivReviewImage.visibility = View.VISIBLE
                ivReviewImage.setImageURI(Uri.parse(review.mediaUri))
            } else {
                ivReviewImage.visibility = View.GONE
            }
            container.addView(view)
        }
    }
}