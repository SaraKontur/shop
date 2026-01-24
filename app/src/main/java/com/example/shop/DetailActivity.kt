package com.example.shop

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import java.util.Date

class DetailActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var productId = 0
    private var tempReviewMediaUri: String? = null
    private var tempMediaType: String? = null // "image" или "video"

    private lateinit var ivPreview: ImageView

    // Лаунчер для выбора медиа
    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                tempReviewMediaUri = uri.toString()

                // Проверяем тип файла
                val type = contentResolver.getType(uri) ?: ""
                if (type.contains("video")) {
                    tempMediaType = "video"
                    ivPreview.setImageResource(android.R.drawable.ic_media_play) // Иконка плеера
                } else {
                    tempMediaType = "image"
                    ivPreview.setImageURI(uri)
                }
                ivPreview.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val name = intent.getStringExtra("NAME") ?: ""
        val price = intent.getDoubleExtra("PRICE", 0.0)
        val desc = intent.getStringExtra("DESC")
        val imageUri = intent.getStringExtra("IMG")
        val userId = intent.getIntExtra("USER_ID", 0)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "shop-db").allowMainThreadQueries().build()

        val product = db.productDao().getProductByName(name) // Ищем по имени
        productId = product?.id ?: 0

        findViewById<TextView>(R.id.detailName).text = name
        findViewById<TextView>(R.id.detailPrice).text = "$price $"
        findViewById<TextView>(R.id.detailDesc).text = desc
        val imgView = findViewById<ImageView>(R.id.detailImage)
        if (imageUri != null) imgView.setImageURI(Uri.parse(imageUri))

        loadReviews()

        findViewById<Button>(R.id.btnAddToCart).setOnClickListener {
            val existingItem = db.productDao().getCartItemByName(name, userId)
            if (existingItem != null) {
                existingItem.quantity += 1
                db.productDao().updateCartItem(existingItem)
            } else {
                val cartItem = CartItem(ownerId = userId, name = name, price = price, imageUri = imageUri, quantity = 1)
                db.productDao().addToCart(cartItem)
            }
            Toast.makeText(this, "В корзине!", Toast.LENGTH_SHORT).show()
        }

        // --- ФОРМА ОТЗЫВА ---
        val ratingBar = findViewById<RatingBar>(R.id.ratingBar)
        val etReview = findViewById<EditText>(R.id.etReview)
        ivPreview = findViewById(R.id.ivReviewPreview)

        // Кнопка прикрепления
        findViewById<Button>(R.id.btnAttachMedia).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*" // Разрешаем всё
            intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*")) // Но фильтруем фото и видео
            pickMediaLauncher.launch(intent)
        }

        findViewById<Button>(R.id.btnSendReview).setOnClickListener {
            val rating = ratingBar.rating.toInt()
            val text = etReview.text.toString()

            if (rating > 0 && text.isNotEmpty()) {
                val user = db.productDao().getUserById(userId)
                val userName = user?.displayName ?: user?.login ?: "Аноним"

                val review = Review(
                    productId = productId,
                    userId = userId,
                    userName = userName,
                    rating = rating,
                    text = text,
                    date = Date().toString(),
                    mediaUri = tempReviewMediaUri,
                    mediaType = tempMediaType
                )
                db.productDao().addReview(review)

                // ОБНОВЛЕНИЕ РЕЙТИНГА И СЧЕТЧИКА
                val newAvg = db.productDao().getAverageRating(productId)
                val newCount = db.productDao().getReviewCount(productId)

                if (product != null) {
                    val updatedProduct = product.copy(rating = newAvg, reviewCount = newCount)
                    db.productDao().updateProduct(updatedProduct)
                }

                Toast.makeText(this, "Отзыв опубликован!", Toast.LENGTH_SHORT).show()
                loadReviews()

                // Сброс полей
                etReview.setText("")
                ratingBar.rating = 0f
                ivPreview.visibility = View.GONE
                tempReviewMediaUri = null
            } else {
                Toast.makeText(this, "Поставьте оценку и текст", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadReviews() {
        val container = findViewById<LinearLayout>(R.id.reviewsContainer)
        container.removeAllViews()

        val reviews = db.productDao().getReviewsForProduct(productId)

        for (review in reviews) {
            // Создаем Layout для одного отзыва
            val reviewLayout = LinearLayout(this)
            reviewLayout.orientation = LinearLayout.VERTICAL
            reviewLayout.setPadding(0, 0, 0, 30)

            // Текст: Имя + Рейтинг
            val tvHeader = TextView(this)
            tvHeader.text = "${review.userName}  ${"⭐".repeat(review.rating)}"
            tvHeader.textSize = 14f
            tvHeader.setTypeface(null, android.graphics.Typeface.BOLD)
            reviewLayout.addView(tvHeader)

            // Текст: Комментарий
            val tvText = TextView(this)
            tvText.text = review.text
            tvText.textSize = 14f
            reviewLayout.addView(tvText)

            // МЕДИА (Если есть)
            if (review.mediaUri != null) {
                val mediaView = ImageView(this)
                val params = LinearLayout.LayoutParams(400, 400)
                params.topMargin = 16
                mediaView.layoutParams = params
                mediaView.scaleType = ImageView.ScaleType.CENTER_CROP

                if (review.mediaType == "video") {
                    mediaView.setImageResource(android.R.drawable.ic_media_play) // Плейсхолдер для видео
                    mediaView.setBackgroundColor(android.graphics.Color.DKGRAY)
                    mediaView.setOnClickListener {
                        // По клику открываем видео во внешнем плеере
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setDataAndType(Uri.parse(review.mediaUri), "video/*")
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        try { startActivity(intent) } catch (e: Exception) {
                            Toast.makeText(this, "Нет плеера", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    mediaView.setImageURI(Uri.parse(review.mediaUri))
                }
                reviewLayout.addView(mediaView)
            }

            container.addView(reviewLayout)
        }

        if (reviews.isEmpty()) {
            val tv = TextView(this)
            tv.text = "Нет отзывов"
            container.addView(tv)
        }
    }
}