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
                
                // 确保数据已填充（升级时保留收藏，仅补 tid/sourceUrl）
                runBlocking {
                    val dao = instance.postDao()
                    val count = dao.getPostCount()
                    if (count == 0) {
                        populateInitialData(dao, context)
                    } else {
                        withContext(Dispatchers.IO) {
                            instance.withTransaction {
                                backfillTidAndSourceUrl(dao, context)
                            }
                        }
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

        // 升级时保留旧数据（含收藏、用户帖子），仅补齐 tid/sourceUrl。
        // 注意：此处的挂起 DAO 方法需在当前 runBlocking 上下文可安全执行。
        // 采用 insertPosts(REPLACE) 全量重写内置数据；仅保留收藏与用户帖子。
        private suspend fun backfillTidAndSourceUrl(dao: PostDao, context: Context) {
            try {
                val favoriteIds = dao.getFavoriteIds().toSet()
                val allPosts = dao.getAll()
                val hasEmptyTid = allPosts.any { it.tid.isBlank() }
                if (!hasEmptyTid) return

                val posts = readBuiltinPosts(context)

                // 用户追加的帖子（不在内置数据中的 id）保留原样，并补一个唯一 tid
                val builtinIds = posts.map { it.id }.toSet()
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

                // 内置数据带新 tid/sourceUrl 重新写入（REPLACE 按 id 覆盖）
                dao.insertPosts(posts)

                // 用户帖子写回（同样 REPLACE）
                if (userPosts.isNotEmpty()) {
                    dao.insertPosts(userPosts)
                }

                // 恢复收藏
                favoriteIds.forEach { id ->
                    dao.setFavoriteFlag(id, true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
