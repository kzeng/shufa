package com.example.shufa.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray

@Database(entities = [PostEntity::class], version = 4, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun postDao(): PostDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shufa_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                populateInitialData(database.postDao(), context)
                            }
                        }
                    }
                    
                    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                        super.onDestructiveMigration(db)
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                populateInitialData(database.postDao(), context)
                            }
                        }
                    }
                })
                .build()
                
                // 确保数据已填充
                runBlocking {
                    val count = instance.postDao().getPostCount()
                    if (count == 0) {
                        populateInitialData(instance.postDao(), context)
                    }
                }
                
                INSTANCE = instance
                instance
            }
        }
        
        private suspend fun populateInitialData(dao: PostDao, context: Context) {
            try {
                // 从 assets/posts.json 导入内置数据
                val json = context.assets.open("posts.json").bufferedReader().use { it.readText() }
                val array = JSONArray(json)
                val posts = (0 until array.length()).map { i ->
                    val obj = array.getJSONObject(i)
                    val imageUrls = obj.optJSONArray("imageUrls")?.let { arr ->
                        (0 until arr.length()).map { arr.getString(it) }
                    } ?: emptyList()
                    val characters = obj.optJSONArray("characters")?.let { arr ->
                        (0 until arr.length()).map { arr.getString(it) }
                    } ?: emptyList()
                    
                    PostEntity(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        author = obj.getString("author"),
                        dynasty = obj.getString("dynasty"),
                        style = obj.getString("style"),
                        description = obj.getString("description"),
                        imageUrls = imageUrls.joinToString(","),
                        characters = characters.joinToString(","),
                        isBuiltIn = true
                    )
                }
                dao.insertPosts(posts)
                
                // 从 filesDir/user_posts.json 导入用户添加的帖子
                val userPostsFile = java.io.File(context.filesDir, "user_posts.json")
                if (userPostsFile.exists()) {
                    val userJson = userPostsFile.readText()
                    val userArray = JSONArray(userJson)
                    val userPosts = (0 until userArray.length()).map { i ->
                        val obj = userArray.getJSONObject(i)
                        val imageUrls = obj.optJSONArray("imageUrls")?.let { arr ->
                            (0 until arr.length()).map { arr.getString(it) }
                        } ?: emptyList()
                        val characters = obj.optJSONArray("characters")?.let { arr ->
                            (0 until arr.length()).map { arr.getString(it) }
                        } ?: emptyList()
                        
                        PostEntity(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            author = obj.getString("author"),
                            dynasty = obj.getString("dynasty"),
                            style = obj.getString("style"),
                            description = obj.getString("description"),
                            imageUrls = imageUrls.joinToString(","),
                            characters = characters.joinToString(","),
                            isBuiltIn = false
                        )
                    }
                    dao.insertPosts(userPosts)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}