package com.example.shufa.data

import android.content.Context
import com.example.shufa.model.CalligraphyPost
import com.example.shufa.model.CalligraphyStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class PostRepository(private val context: Context) {

    private var cachedPosts: List<CalligraphyPost>? = null

    suspend fun getPosts(): List<CalligraphyPost> {
        cachedPosts?.let { return it }
        return withContext(Dispatchers.IO) {
            val json = context.assets.open("posts.json").bufferedReader().use { it.readText() }
            val posts = parsePosts(json)
            cachedPosts = posts
            posts
        }
    }

    suspend fun getPostById(id: String): CalligraphyPost? {
        return getPosts().find { it.id == id }
    }

    private fun parsePosts(json: String): List<CalligraphyPost> {
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            val charactersArray = obj.optJSONArray("characters")
            val characters = if (charactersArray != null) {
                (0 until charactersArray.length()).map { charactersArray.getString(it) }
            } else {
                emptyList()
            }
            CalligraphyPost(
                id = obj.getString("id"),
                title = obj.getString("title"),
                author = obj.getString("author"),
                dynasty = obj.getString("dynasty"),
                style = CalligraphyStyle.valueOf(obj.getString("style")),
                description = obj.getString("description"),
                imageUrl = obj.optString("imageUrl", ""),
                characters = characters
            )
        }
    }
}
