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

    // Список категорий
    private val categories = arrayOf("Электроника", "Одежда", "Еда", "Разное")

    private val pickProductImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                productMediaUri = uri.toString()

                // Когда выбрали фото - убираем заглушку и делаем красиво
                val iv = findViewById<ImageView>(R.id.detailImage)
                iv.setImageURI(uri)
                iv.scaleType = ImageView.ScaleType.CENTER_CROP
                iv.setPadding(0,0,0,0) // Убираем отступы заглушки
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
        val currentUserId = intent.getIntExtra("USER_ID", 0)

        val ivImage = findViewById<ImageView>(R.id.detailImage)
        val btnUpload = findViewById<Button>(R.id.btnUploadImage)

        val tvName = findViewById<TextView>(R.id.detailName)
        val tvPrice = findViewById<TextView>(R.id.detailPrice)
        val tvDesc = findViewById<TextView>(R.id.detailDesc)

        val etName = findViewById<EditText>(R.id.etName)
        val etPrice = findViewById<EditText>(R.id.etPrice)
        val etDesc = findViewById<EditText>(R.id.etDesc)

        // Настройка спиннера (выпадающего списка)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        val btnCart = findViewById<Button>(R.id.btnAddToCart)
        val btnSave = findViewById<Button>(R.id.btnSaveProduct)
        val reviewsBlock = findViewById<LinearLayout>(R.id.reviewsBlock)

        if (isEditMode) {
            // РЕЖИМ СОЗДАНИЯ
            etName.visibility = View.VISIBLE
            etPrice.visibility = View.VISIBLE
            etDesc.visibility = View.VISIBLE
            spinnerCategory.visibility = View.VISIBLE // Показываем спиннер
            btnUpload.visibility = View.VISIBLE
            btnSave.visibility = View.VISIBLE

            tvName.visibility = View.GONE
            tvPrice.visibility = View.GONE
            tvDesc.visibility = View.GONE
            btnCart.visibility = View.GONE
            reviewsBlock.visibility = View.GONE

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
                val desc = etDesc.text.toString()
                // Берем выбранную категорию из спиннера
                val cat = spinnerCategory.selectedItem.toString()

                if (name.isNotEmpty() && priceStr.isNotEmpty()) {
                    val newProduct = Product(
                        name = name,
                        price = priceStr.toDoubleOrNull() ?: 0.0,
                        description = desc,
                        category = cat, // Используем значение из спиннера
                        imageUri = productMediaUri,
                        rating = 0f,
                        reviewCount = 0
                    )
                    db.productDao().insertProduct(newProduct)
                    Toast.makeText(this, "Товар создан!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Заполните название и цену", Toast.LENGTH_SHORT).show()
                }
            }

        } else {
            // РЕЖИМ ПРОСМОТРА
            val name = intent.getStringExtra("NAME") ?: ""
            val product = db.productDao().getProductByName(name)
            productId = product?.id ?: 0

            tvName.text = product?.name
            tvPrice.text = "${product?.price} $"
            tvDesc.text = product?.description

            if (product?.imageUri != null) {
                ivImage.setImageURI(Uri.parse(product.imageUri))
                ivImage.scaleType = ImageView.ScaleType.CENTER_CROP
                ivImage.setPadding(0,0,0,0)
            }

            btnCart.setOnClickListener {
                val existingItem = db.productDao().getCartItemByName(name, currentUserId)
                if (existingItem != null) {
                    existingItem.quantity += 1
                    db.productDao().updateCartItem(existingItem)
                } else {
                    val cartItem = CartItem(ownerId = currentUserId, name = name, price = product?.price ?: 0.0, imageUri = product?.imageUri, quantity = 1)
                    db.productDao().addToCart(cartItem)
                }
                Toast.makeText(this, "В корзине!", Toast.LENGTH_SHORT).show()
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
                val text = findViewById<EditText>(R.id.etReview).text.toString()
                if (rating > 0) {
                    val user = db.productDao().getUserById(currentUserId)
                    val userName = user?.displayName ?: user?.login ?: "Аноним"

                    // Красивая дата
                    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    val currentDate = sdf.format(Date())

                    val review = Review(productId = productId, userId = currentUserId, userName = userName, rating = rating, text = text, date = currentDate, mediaUri = reviewMediaUri, mediaType = reviewMediaType)
                    db.productDao().addReview(review)

                    if (product != null) {
                        val newAvg = db.productDao().getAverageRating(productId)
                        val newCount = db.productDao().getReviewCount(productId)
                        db.productDao().updateProduct(product.copy(rating = newAvg, reviewCount = newCount))
                    }

                    Toast.makeText(this, "Отзыв отправлен", Toast.LENGTH_SHORT).show()
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
            // Инфлейтим (создаем) красивую карточку из item_review.xml
            val view = LayoutInflater.from(this).inflate(R.layout.item_review, container, false)

            val tvAuthor = view.findViewById<TextView>(R.id.reviewAuthor)
            val tvDate = view.findViewById<TextView>(R.id.reviewDate)
            val tvText = view.findViewById<TextView>(R.id.reviewText)
            val ratingBar = view.findViewById<RatingBar>(R.id.reviewRating)

            tvAuthor.text = review.userName
            // Показываем дату (если в базе старые отзывы с плохой датой, просто покажем что есть)
            tvDate.text = review.date
            tvText.text = review.text
            ratingBar.rating = review.rating.toFloat()

            container.addView(view)
        }
    }
}