package com.example.shop

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val login: String,
    val pass: String,
    val isAdmin: Boolean,
    val displayName: String = "",
    val avatarUri: String? = null
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val price: Double,
    val description: String,
    val imageUri: String? = null,
    val category: String = "Разное",
    val rating: Float = 0.0f,
    val reviewCount: Int = 0 // НОВОЕ: Количество отзывов
)

@Entity(tableName = "reviews")
data class Review(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val userId: Int,
    val userName: String,
    val rating: Int,
    val text: String,
    val date: String,
    val mediaUri: String? = null, // НОВОЕ: Ссылка на медиа
    val mediaType: String? = null // НОВОЕ: "image" или "video"
)

@Entity(tableName = "cart")
data class CartItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ownerId: Int,
    val name: String,
    val price: Double,
    val imageUri: String?,
    var quantity: Int = 1
)

@Entity(tableName = "favorites")
data class FavoriteItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ownerId: Int,
    val name: String,
    val price: Double,
    val imageUri: String?
)

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ownerId: Int,
    val date: String,
    val itemsSummary: String,
    val totalPrice: Double
)