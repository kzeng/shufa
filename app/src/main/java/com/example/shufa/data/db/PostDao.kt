package com.example.shufa.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {

    @Query("SELECT * FROM posts ORDER BY title ASC")
    fun getAllPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts")
    suspend fun getAll(): List<PostEntity>

    @Query("SELECT * FROM posts WHERE id = :id")
    suspend fun getPostById(id: String): PostEntity?

    @Query("SELECT * FROM posts WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoritePosts(): Flow<List<PostEntity>>

    @Query("UPDATE posts SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("""
        SELECT * FROM posts
        WHERE title LIKE '%' || :query || '%'
        OR author LIKE '%' || :query || '%'
        OR dynasty LIKE '%' || :query || '%'
        OR style LIKE '%' || :query || '%'
        OR description LIKE '%' || :query || '%'
        ORDER BY title ASC
    """)
    fun searchPosts(query: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE style = :style ORDER BY title ASC")
    fun getPostsByStyle(style: String): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Update
    suspend fun updatePost(post: PostEntity)

    @Query("DELETE FROM posts WHERE id = :id")
    suspend fun deletePostById(id: String)

    @Query("SELECT COUNT(*) FROM posts")
    suspend fun getPostCount(): Int

    @Query("SELECT COUNT(*) FROM posts WHERE id = :id")
    suspend fun isPostExists(id: String): Boolean

    @Query("SELECT tid FROM posts")
    suspend fun getAllTids(): List<String>

    @Query("SELECT id FROM posts WHERE isFavorite = 1")
    suspend fun getFavoriteIds(): List<String>

    @Query("UPDATE posts SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavoriteFlag(id: String, isFavorite: Boolean)
}
