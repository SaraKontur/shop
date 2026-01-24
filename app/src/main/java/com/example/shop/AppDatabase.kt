package com.example.shop

import androidx.room.*

@Dao
interface ProductDao {
    // --- ПОЛЬЗОВАТЕЛИ ---
    @Insert fun registerUser(user: User)
    @Query("SELECT * FROM users WHERE login = :login AND pass = :pass LIMIT 1") fun loginUser(login: String, pass: String): User?
    @Query("SELECT * FROM users WHERE login = :login LIMIT 1") fun checkUserExists(login: String): User?
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1") fun getUserById(id: Int): User?
    @Update fun updateUser(user: User)

    // --- ТОВАРЫ ---
    @Query("SELECT * FROM products") fun getAllProducts(): List<Product>
    @Query("SELECT * FROM products WHERE name LIKE '%' || :search || '%' AND (:category = 'Все' OR category = :category)")
    fun searchProducts(search: String, category: String): List<Product>

    // НОВОЕ: Найти реальный товар по имени (для Favorites)
    @Query("SELECT * FROM products WHERE name = :name LIMIT 1")
    fun getProductByName(name: String): Product?

    @Insert fun insertProduct(product: Product)
    @Delete fun deleteProduct(product: Product)
    @Update fun updateProduct(product: Product)

    // --- ОТЗЫВЫ ---
    @Insert fun addReview(review: Review)
    @Query("SELECT * FROM reviews WHERE productId = :productId ORDER BY id DESC") fun getReviewsForProduct(productId: Int): List<Review>
    @Query("SELECT AVG(rating) FROM reviews WHERE productId = :productId") fun getAverageRating(productId: Int): Float
    @Query("SELECT COUNT(*) FROM reviews WHERE productId = :productId") fun getReviewCount(productId: Int): Int

    // --- КОРЗИНА ---
    @Query("SELECT * FROM cart WHERE ownerId = :userId") fun getCartItems(userId: Int): List<CartItem>
    @Query("SELECT * FROM cart WHERE name = :name AND ownerId = :userId LIMIT 1") fun getCartItemByName(name: String, userId: Int): CartItem?
    @Insert fun addToCart(item: CartItem)
    @Update fun updateCartItem(item: CartItem)
    @Delete fun deleteCartItem(item: CartItem)
    @Query("DELETE FROM cart WHERE ownerId = :userId") fun clearCart(userId: Int)

    // --- ИЗБРАННОЕ ---
    @Query("SELECT * FROM favorites WHERE ownerId = :userId") fun getFavItems(userId: Int): List<FavoriteItem>
    @Insert fun addToFav(item: FavoriteItem)
    @Query("DELETE FROM favorites WHERE name = :name AND ownerId = :userId") fun removeFromFav(name: String, userId: Int)
    @Query("SELECT EXISTS(SELECT * FROM favorites WHERE name = :name AND ownerId = :userId)") fun isFavorite(name: String, userId: Int): Boolean

    // --- ЗАКАЗЫ ---
    @Insert
    fun insertOrder(order: Order)

    // Для обычного юзера (только свои)
    @Query("SELECT * FROM orders WHERE ownerId = :userId ORDER BY id DESC")
    fun getUserOrders(userId: Int): List<Order>

    // НОВОЕ: Для Админа (вообще все заказы)
    @Query("SELECT * FROM orders ORDER BY id DESC")
    fun getAllOrders(): List<Order>

    // НОВОЕ: Обновить статус заказа
    @Update
    fun updateOrder(order: Order)
}

// ВЕРСИЯ 8
@Database(entities = [User::class, Product::class, CartItem::class, FavoriteItem::class, Order::class, Review::class], version = 10, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
}