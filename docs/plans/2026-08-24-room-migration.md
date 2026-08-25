# Room 数据库迁移方案

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将书法字帖应用从 JSON 文件存储迁移到 Room 数据库，提升大数据量下的性能和查询能力。

**Architecture:** 
- 使用 Room 作为本地数据库
- 保留 JSON 作为初始数据导入源
- Repository 层统一管理数据访问
- 支持从 JSON 初始化数据到 Room

**Tech Stack:** 
- Room (androidx.room)
- Kotlin Coroutines
- Flow (响应式查询)
- KSP (注解处理)

---

## 当前数据结构分析

**CalligraphyPost 数据模型：**
- id: String (唯一标识)
- title: String (碑帖名称)
- author: String (作者)
- dynasty: String (朝代)
- style: CalligraphyStyle (字体枚举: KAISHU, XINGSHU, CAOSHU, LISHU, ZHUANSHU)
- description: String (详细介绍)
- imageUrls: List<String> (图片URL列表)
- characters: List<String> (代表字列表)

**当前存储方式：**
- assets/posts.json: 内置碑帖数据 (只读)
- filesDir/user_posts.json: 用户添加的碑帖 (可写)

---

## Room 优势

1. **性能优化**: 大数据量下查询更快
2. **索引支持**: 支持按标题、作者、朝代、字体等字段索引
3. **响应式查询**: 使用 Flow 实现数据变化自动通知 UI
4. **数据完整性**: 支持约束、唯一性检查
5. **迁移支持**: 版本升级时数据迁移
6. **线程安全**: 内置线程管理

---

## 迁移步骤

### Task 1: 添加 Room 依赖

**Files:**
- Modify: `app/build.gradle.kts`

**Step 1: 添加 Room 依赖**

```kotlin
// Room
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")
```

**Step 2: 添加 KSP 插件**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp") version "2.0.21-1.0.27"
}

// 在 android 块中添加
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

**Step 3: 同步 Gradle**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

### Task 2: 创建数据库实体

**Files:**
- Create: `app/src/main/java/com/example/shufa/data/db/PostEntity.kt`

**Step 1: 创建实体类**

```kotlin
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
    val style: String, // 存储枚举名称
    val description: String,
    val imageUrls: String, // JSON数组字符串
    val characters: String, // JSON数组字符串
    val isBuiltIn: Boolean = true // 是否为内置数据
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
```

**Step 2: 验证编译**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

### Task 3: 创建 DAO

**Files:**
- Create: `app/src/main/java/com/example/shufa/data/db/PostDao.kt`

**Step 1: 创建 DAO 接口**

```kotlin
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
    
    @Query("SELECT * FROM posts WHERE id = :id")
    suspend fun getPostById(id: String): PostEntity?
    
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
}
```

**Step 2: 验证编译**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

### Task 4: 创建数据库类

**Files:**
- Create: `app/src/main/java/com/example/shufa/data/db/AppDatabase.kt`

**Step 1: 创建数据库类**

```kotlin
package com.example.shufa.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.shufa.model.CalligraphyPost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

@Database(entities = [PostEntity::class], version = 1, exportSchema = true)
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
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                populateInitialData(database.postDao(), context)
                            }
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        private suspend fun populateInitialData(dao: PostDao, context: Context) {
            try {
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
```

**Step 2: 验证编译**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

### Task 5: 更新 Repository

**Files:**
- Modify: `app/src/main/java/com/example/shufa/data/PostRepository.kt`

**Step 1: 重写 Repository 使用 Room**

```kotlin
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
    
    // 获取所有帖子（响应式）
    fun getAllPosts(): Flow<List<CalligraphyPost>> {
        return postDao.getAllPosts().map { entities ->
            entities.map { it.toCalligraphyPost() }
        }
    }
    
    // 搜索帖子（响应式）
    fun searchPosts(query: String): Flow<List<CalligraphyPost>> {
        return postDao.searchPosts(query).map { entities ->
            entities.map { it.toCalligraphyPost() }
        }
    }
    
    // 按风格筛选（响应式）
    fun getPostsByStyle(style: CalligraphyStyle): Flow<List<CalligraphyPost>> {
        return postDao.getPostsByStyle(style.name).map { entities ->
            entities.map { it.toCalligraphyPost() }
        }
    }
    
    // 根据ID获取帖子
    suspend fun getPostById(id: String): CalligraphyPost? {
        return withContext(Dispatchers.IO) {
            postDao.getPostById(id)?.toCalligraphyPost()
        }
    }
    
    // 添加帖子
    suspend fun addPost(post: CalligraphyPost) {
        withContext(Dispatchers.IO) {
            val entity = PostEntity.fromCalligraphyPost(post, isBuiltIn = false)
            postDao.insertPost(entity)
        }
    }
    
    // 批量添加帖子
    suspend fun addPosts(posts: List<CalligraphyPost>) {
        withContext(Dispatchers.IO) {
            val entities = posts.map { PostEntity.fromCalligraphyPost(it, isBuiltIn = false) }
            postDao.insertPosts(entities)
        }
    }
    
    // 获取帖子总数
    suspend fun getPostCount(): Int {
        return withContext(Dispatchers.IO) {
            postDao.getPostCount()
        }
    }
    
    // 检查帖子是否存在
    suspend fun isPostExists(id: String): Boolean {
        return withContext(Dispatchers.IO) {
            postDao.isPostExists(id)
        }
    }
}
```

**Step 2: 验证编译**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

### Task 6: 更新 ViewModel

**Files:**
- Modify: `app/src/main/java/com/example/shufa/ui/select/SelectViewModel.kt`

**Step 1: 更新 SelectViewModel 使用 Flow**

```kotlin
package com.example.shufa.ui.select

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shufa.data.PostRepository
import com.example.shufa.model.CalligraphyPost
import com.example.shufa.model.CalligraphyStyle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class SelectUiState(
    val posts: List<CalligraphyPost> = emptyList(),
    val selectedStyle: CalligraphyStyle? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val networkResults: List<CalligraphyPost> = emptyList(),
    val isSearching: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class SelectViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = PostRepository(application)
    
    private val _uiState = MutableStateFlow(SelectUiState())
    val uiState: StateFlow<SelectUiState> = _uiState.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    private val _selectedStyle = MutableStateFlow<CalligraphyStyle?>(null)
    
    init {
        loadPosts()
    }
    
    private fun loadPosts() {
        viewModelScope.launch {
            // 使用 flatMapLatest 响应查询变化
            _searchQuery.flatMapLatest { query ->
                if (query.isEmpty()) {
                    _selectedStyle.flatMapLatest { style ->
                        if (style == null) {
                            repository.getAllPosts()
                        } else {
                            repository.getPostsByStyle(style)
                        }
                    }
                } else {
                    repository.searchPosts(query)
                }
            }.map { posts ->
                SelectUiState(
                    posts = posts,
                    selectedStyle = _selectedStyle.value,
                    searchQuery = _searchQuery.value,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
    
    fun filterByStyle(style: CalligraphyStyle?) {
        _selectedStyle.value = style
        _uiState.value = _uiState.value.copy(selectedStyle = style)
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.value = _uiState.value.copy(searchQuery = query)
        if (query.length >= 2) {
            searchNetwork(query)
        }
    }
    
    private fun searchNetwork(query: String) {
        // 保留网络搜索逻辑
        _uiState.value = _uiState.value.copy(isSearching = true)
        viewModelScope.launch {
            // ... 网络搜索实现
        }
    }
    
    fun addNetworkPost(post: CalligraphyPost) {
        viewModelScope.launch {
            repository.addPost(post)
            _uiState.value = _uiState.value.copy(
                networkResults = _uiState.value.networkResults.filter { it.id != post.id }
            )
        }
    }
}
```

**Step 2: 验证编译**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

### Task 7: 更新 ViewViewModel

**Files:**
- Modify: `app/src/main/java/com/example/shufa/ui/view/ViewViewModel.kt`

**Step 1: 更新 ViewViewModel**

```kotlin
package com.example.shufa.ui.view

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shufa.data.PostRepository
import com.example.shufa.model.CalligraphyPost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ViewUiState(
    val post: CalligraphyPost? = null,
    val isLoading: Boolean = true
)

class ViewViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = PostRepository(application)
    
    private val _uiState = MutableStateFlow(ViewUiState())
    val uiState: StateFlow<ViewUiState> = _uiState.asStateFlow()
    
    fun loadPost(postId: String) {
        viewModelScope.launch {
            _uiState.value = ViewUiState(isLoading = true)
            val post = repository.getPostById(postId)
            _uiState.value = ViewUiState(
                post = post,
                isLoading = false
            )
        }
    }
}
```

**Step 2: 验证编译**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

---

### Task 8: 测试和验证

**Step 1: 构建并安装**

Run: 
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.example.shufa/.MainActivity
```

**Step 2: 验证功能**
- [ ] 选贴页面显示所有帖子
- [ ] 搜索功能正常工作
- [ ] 风格筛选正常工作
- [ ] 帖子详情页正常显示
- [ ] 网络搜索添加帖子功能正常
- [ ] 分页功能正常

**Step 3: 检查数据库**

Run: 
```bash
adb shell run-as com.example.shufa ls databases/
adb shell run-as com.example.shufa cat databases/shufa_database
```

---

## 性能对比

### JSON 方案
- 启动时加载整个 JSON 文件
- 搜索需要遍历所有数据
- 不支持响应式更新
- 大数据量下性能差

### Room 方案
- 数据库查询，支持索引
- 支持 Flow 响应式更新
- 支持复杂查询（LIKE, AND, OR）
- 大数据量下性能优秀

---

## 注意事项

1. **数据迁移**: 首次启动时从 JSON 导入数据到 Room
2. **版本管理**: Room 支持数据库版本升级和迁移
3. **线程安全**: 使用协程和 Flow 确保线程安全
4. **内存优化**: Flow 只在数据变化时通知 UI
5. **测试**: 建议添加单元测试验证 DAO 和 Repository

---

## 后续优化

1. **分页加载**: 使用 Paging 3 库实现分页
2. **缓存策略**: 添加内存缓存提升性能
3. **数据同步**: 支持云端同步
4. **全文搜索**: 使用 FTS4 支持全文搜索
5. **数据导出**: 支持导出为 JSON 格式
