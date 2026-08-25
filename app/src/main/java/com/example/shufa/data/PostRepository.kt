package com.example.shufa.data

import android.content.Context
import com.example.shufa.data.db.AppDatabase
import com.example.shufa.data.db.PostEntity
import com.example.shufa.model.CalligraphyPost
import com.example.shufa.model.CalligraphyStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PostRepository(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val postDao = database.postDao()

    fun getAllPosts(): Flow<List<CalligraphyPost>> {
        return postDao.getAllPosts().map { entities ->
            entities.map { it.toCalligraphyPost() }
        }
    }

    fun searchPosts(query: String): Flow<List<CalligraphyPost>> {
        return postDao.searchPosts(query).map { entities ->
            entities.map { it.toCalligraphyPost() }
        }
    }

    fun getPostsByStyle(style: CalligraphyStyle): Flow<List<CalligraphyPost>> {
        return postDao.getPostsByStyle(style.name).map { entities ->
            entities.map { it.toCalligraphyPost() }
        }
    }

    suspend fun getPostById(id: String): CalligraphyPost? {
        return withContext(Dispatchers.IO) {
            postDao.getPostById(id)?.toCalligraphyPost()
        }
    }

    suspend fun addPost(post: CalligraphyPost) {
        withContext(Dispatchers.IO) {
            val entity = PostEntity.fromCalligraphyPost(post, isBuiltIn = false)
            postDao.insertPost(entity)
        }
    }

    suspend fun addPosts(posts: List<CalligraphyPost>) {
        withContext(Dispatchers.IO) {
            val entities = posts.map { PostEntity.fromCalligraphyPost(it, isBuiltIn = false) }
            postDao.insertPosts(entities)
        }
    }

    suspend fun getPostCount(): Int {
        return withContext(Dispatchers.IO) {
            postDao.getPostCount()
        }
    }

    suspend fun isPostExists(id: String): Boolean {
        return withContext(Dispatchers.IO) {
            postDao.isPostExists(id)
        }
    }
}
