package com.example.shufa.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.shufa.model.CalligraphyPost
import com.example.shufa.model.CalligraphyStyle

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey
    val id: String,
    val tid: String,
    val title: String,
    val author: String,
    val dynasty: String,
    val style: String,
    val description: String,
    val imageUrls: String,
    val characters: String,
    val sourceUrl: String = "",
    val isBuiltIn: Boolean = true,
    val isFavorite: Boolean = false
) {
    fun toCalligraphyPost(): CalligraphyPost {
        return CalligraphyPost(
            id = id,
            tid = tid,
            title = title,
            author = author,
            dynasty = dynasty,
            style = CalligraphyStyle.valueOf(style),
            description = description,
            imageUrls = parseJsonArray(imageUrls),
            characters = parseJsonArray(characters),
            sourceUrl = parseJsonArray(sourceUrl),
            isFavorite = isFavorite
        )
    }

    companion object {
        fun fromCalligraphyPost(post: CalligraphyPost, isBuiltIn: Boolean = true): PostEntity {
            return PostEntity(
                id = post.id,
                tid = post.tid,
                title = post.title,
                author = post.author,
                dynasty = post.dynasty,
                style = post.style.name,
                description = post.description,
                imageUrls = post.imageUrls.joinToString(","),
                characters = post.characters.joinToString(","),
                sourceUrl = post.sourceUrl.joinToString(","),
                isBuiltIn = isBuiltIn,
                isFavorite = post.isFavorite
            )
        }

        private fun parseJsonArray(json: String): List<String> {
            return if (json.isBlank()) emptyList()
            else json.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }
}
