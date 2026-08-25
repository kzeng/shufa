package com.example.shufa.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.shufa.model.CalligraphyPost
import com.example.shufa.model.CalligraphyStyle

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val author: String,
    val dynasty: String,
    val style: String,
    val description: String,
    val imageUrls: String,
    val characters: String,
    val isBuiltIn: Boolean = true
) {
    fun toCalligraphyPost(): CalligraphyPost {
        return CalligraphyPost(
            id = id,
            title = title,
            author = author,
            dynasty = dynasty,
            style = CalligraphyStyle.valueOf(style),
            description = description,
            imageUrls = parseJsonArray(imageUrls),
            characters = parseJsonArray(characters)
        )
    }

    companion object {
        fun fromCalligraphyPost(post: CalligraphyPost, isBuiltIn: Boolean = true): PostEntity {
            return PostEntity(
                id = post.id,
                title = post.title,
                author = post.author,
                dynasty = post.dynasty,
                style = post.style.name,
                description = post.description,
                imageUrls = post.imageUrls.joinToString(","),
                characters = post.characters.joinToString(","),
                isBuiltIn = isBuiltIn
            )
        }

        private fun parseJsonArray(json: String): List<String> {
            return if (json.isBlank()) emptyList()
            else json.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }
}
