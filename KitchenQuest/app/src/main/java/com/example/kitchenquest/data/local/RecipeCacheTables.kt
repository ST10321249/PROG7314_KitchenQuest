package com.example.kitchenquest.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

// A copy of a recipe the user opened, so it can still be read without a connection.
// The whole recipe (ingredients and steps) is stored as JSON because the screens only
// ever need it as one piece. Rows always belong to one user so accounts stay separate.
@Entity(
    tableName = "cached_recipes",
    primaryKeys = ["userId", "recipeSourceId"],
    indices = [Index("userId", "savedAt")]
)
data class CachedRecipeEntity(
    val userId: String,
    val recipeSourceId: String,
    val title: String,
    val recipeJson: String,
    // When this copy was last refreshed from the server (milliseconds).
    val savedAt: Long
)

// The user's saved (favourite) recipes as the server last returned them, so the
// Saved Recipes screen can be shown offline. "listOrder" keeps the server's order.
@Entity(
    tableName = "cached_favourites",
    primaryKeys = ["userId", "recipeSourceId"]
)
data class CachedFavouriteEntity(
    val userId: String,
    val recipeSourceId: String,
    val favouriteId: String,
    val recipeTitle: String,
    val imageUrl: String? = null,
    val listOrder: Int
)

// An abstract class (not an interface) so Room can run replaceFavourites as one transaction.
@Dao
abstract class RecipeCacheDao {

    @Query("SELECT * FROM cached_recipes WHERE userId = :userId AND recipeSourceId = :recipeSourceId")
    abstract suspend fun getRecipe(userId: String, recipeSourceId: String): CachedRecipeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun putRecipe(recipe: CachedRecipeEntity)

    @Query("SELECT COUNT(*) FROM cached_recipes WHERE userId = :userId")
    abstract suspend fun countRecipes(userId: String): Int

    // Keeps the newest "keep" copies plus every saved recipe, and removes the rest,
    // so the cache cannot grow without limit.
    @Query(
        """
        DELETE FROM cached_recipes
        WHERE userId = :userId
          AND recipeSourceId NOT IN (SELECT recipeSourceId FROM cached_favourites WHERE userId = :userId)
          AND recipeSourceId NOT IN (
              SELECT recipeSourceId FROM cached_recipes
              WHERE userId = :userId
              ORDER BY savedAt DESC
              LIMIT :keep
          )
        """
    )
    abstract suspend fun trimRecipes(userId: String, keep: Int)

    @Query("SELECT * FROM cached_favourites WHERE userId = :userId ORDER BY listOrder")
    abstract suspend fun getFavourites(userId: String): List<CachedFavouriteEntity>

    @Query("SELECT COALESCE(MAX(listOrder), -1) FROM cached_favourites WHERE userId = :userId")
    abstract suspend fun lastFavouriteOrder(userId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun putFavourites(favourites: List<CachedFavouriteEntity>)

    @Query("DELETE FROM cached_favourites WHERE userId = :userId AND recipeSourceId = :recipeSourceId")
    abstract suspend fun removeFavourite(userId: String, recipeSourceId: String)

    @Query("DELETE FROM cached_favourites WHERE userId = :userId")
    abstract suspend fun clearFavourites(userId: String)

    // Swaps the whole saved list in one step, so a half-written list is never shown.
    @Transaction
    open suspend fun replaceFavourites(userId: String, favourites: List<CachedFavouriteEntity>) {
        clearFavourites(userId)
        putFavourites(favourites)
    }
}class RecipeCacheTables {
}