package com.example.shufa.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import com.example.shufa.data.TidUtils

@Database(entities = [PostEntity::class], version = 6, exportSchema = false)
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
                .addMigrations(MIGRATION_5_6)
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
                
                // 按 id 补差集导入：每次启动都用 posts.json 全量 REPLACE 内置帖，
                // 同时保留用户自建帖和收藏。对老用户升级自动补齐新帖。
                runBlocking {
                    val dao = instance.postDao()
                    try {
                        instance.withTransaction {
                            mergeBuiltinPosts(dao, context)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                INSTANCE = instance
                instance
            }
        }
        
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE posts ADD COLUMN tid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE posts ADD COLUMN sourceUrl TEXT NOT NULL DEFAULT ''")
            }
        }

        // 按 id 补差集导入内置帖：读取 posts.json → REPLACE 内置帖（自动插入新帖、覆盖更新旧帖），
        // 保留用户自建帖（id 不在内置集合中），恢复收藏，补齐用户帖的 tid。
        private suspend fun mergeBuiltinPosts(dao: PostDao, context: Context) {
            val posts = readBuiltinPosts(context)
            val builtinIds = posts.map { it.id }.toSet()

            // 保存收藏状态（REPLACE 会将 isFavorite 重置为 false）
            val favoriteIds = dao.getFavoriteIds().toSet()

            // 用 REPLACE 全量写入内置帖：新增的帖被插入，已存在的帖被覆盖（description、图片 URL、tid 等同步更新）
            dao.insertPosts(posts)

            // 补齐用户自建帖的 tid（不在内置集合中的帖）
            val allPosts = dao.getAll()
            val usedTids = allPosts.mapNotNull { it.tid.takeIf { t -> t.isNotBlank() } }.toMutableSet()
            val userPosts = allPosts
                .filter { it.id !in builtinIds }
                .map { p ->
                    val tid = if (p.tid.isNotBlank()) p.tid else {
                        val n = TidUtils.generate(usedTids)
                        usedTids.add(n)
                        n
                    }
                    p.copy(tid = tid)
                }
            if (userPosts.isNotEmpty()) {
                dao.insertPosts(userPosts)
            }

            // 恢复收藏
            favoriteIds.forEach { id ->
                dao.setFavoriteFlag(id, true)
            }
        }

        private suspend fun readBuiltinPosts(context: Context): List<PostEntity> {
            val json = context.assets.open("posts.json").bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            return (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                val imageUrls = obj.optJSONArray("imageUrls")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList()
                val characters = obj.optJSONArray("characters")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList()
                val sourceUrl = obj.optJSONArray("sourceUrl")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList()
                PostEntity(
                    id = obj.getString("id"),
                    tid = obj.optString("tid", ""),
                    title = obj.getString("title"),
                    author = obj.getString("author"),
                    dynasty = obj.getString("dynasty"),
                    style = obj.getString("style"),
                    description = obj.getString("description"),
                    imageUrls = imageUrls.joinToString(","),
                    characters = characters.joinToString(","),
                    sourceUrl = sourceUrl.joinToString(","),
                    isBuiltIn = true,
                    isFavorite = false
                )
            }
        }

        private suspend fun populateInitialData(dao: PostDao, context: Context) {
            try {
                // 从 assets/posts.json 导入内置数据
                val posts = readBuiltinPosts(context)
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
                        val sourceUrl = obj.optJSONArray("sourceUrl")?.let { arr ->
                            (0 until arr.length()).map { arr.getString(it) }
                        } ?: emptyList()
                        
                        PostEntity(
                            id = obj.getString("id"),
                            tid = obj.optString("tid", ""),
                            title = obj.getString("title"),
                            author = obj.getString("author"),
                            dynasty = obj.getString("dynasty"),
                            style = obj.getString("style"),
                            description = obj.getString("description"),
                            imageUrls = imageUrls.joinToString(","),
                            characters = characters.joinToString(","),
                            sourceUrl = sourceUrl.joinToString(","),
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
