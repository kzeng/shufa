# V1 Implementation Plan

> **For OpenCode:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 搭建 Android 书法学习 APP 骨架，实现选贴与看贴两个核心功能

**Architecture:** 单 Activity + Jetpack Compose，MVVM 架构，Compose Navigation 管理页面跳转。数据暂用本地 JSON，后续可切换网络。

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Compose Navigation, Gradle Kotlin DSL, AGP 8.x

---

## Task 1: 初始化 Gradle 项目结构

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/wrapper/gradle-wrapper.properties`

**Step 1: 创建 settings.gradle.kts**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Shufa"
include(":app")
```

**Step 2: 创建根 build.gradle.kts**

```kotlin
plugins {
    id("com.android.application") version "8.7.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
```

**Step 3: 创建 gradle.properties**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

**Step 4: 创建 gradle-wrapper.properties**

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

**Step 5: Commit**

```bash
git init
git add settings.gradle.kts build.gradle.kts gradle.properties gradle/
git commit -m "chore: init gradle project structure"
```

---

## Task 2: 创建 app 模块

**Files:**
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/proguard-rules.pro`

**Step 1: 创建 app/build.gradle.kts**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.shufa"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.shufa"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Material 3
    implementation("androidx.compose.material3:material3")

    // Activity & ViewModel
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Core
    implementation("androidx.core:core-ktx:1.15.0")
}
```

**Step 2: 创建 AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="true"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.Shufa">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Shufa">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

**Step 3: 创建 proguard-rules.pro**

空文件，保留占位。

**Step 4: Commit**

```bash
git add app/
git commit -m "feat: add app module with Compose + M3 dependencies"
```

---

## Task 3: 创建资源文件与主题

**Files:**
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/java/com/example/shufa/ui/theme/Color.kt`
- Create: `app/src/main/java/com/example/shufa/ui/theme/Type.kt`
- Create: `app/src/main/java/com/example/shufa/ui/theme/Theme.kt`

**Step 1: 创建 strings.xml**

```xml
<resources>
    <string name="app_name">书法学习</string>
</resources>
```

**Step 2: 创建 themes.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Shufa" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

**Step 3: 创建 Color.kt**

```kotlin
package com.example.shufa.ui.theme

import androidx.compose.ui.graphics.Color

val Brown50 = Color(0xFFEFEBE9)
val Brown100 = Color(0xFFD7CCC8)
val Brown500 = Color(0xFF795548)
val Brown700 = Color(0xFF5D4037)
val Brown900 = Color(0xFF3E2723)

val Gold500 = Color(0xFFFFC107)
val Gold700 = Color(0xFFFFA000)

val PaperWhite = Color(0xFFFAFAF5)
val InkBlack = Color(0xFF1A1A1A)
```

**Step 4: 创建 Type.kt**

```kotlin
package com.example.shufa.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)
```

**Step 5: 创建 Theme.kt**

```kotlin
package com.example.shufa.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Brown500,
    onPrimary = PaperWhite,
    primaryContainer = Brown100,
    onPrimaryContainer = Brown900,
    secondary = Gold500,
    onSecondary = InkBlack,
    secondaryContainer = Gold700,
    background = PaperWhite,
    onBackground = InkBlack,
    surface = PaperWhite,
    onSurface = InkBlack,
)

private val DarkColorScheme = darkColorScheme(
    primary = Brown100,
    onPrimary = Brown900,
    primaryContainer = Brown700,
    onPrimaryContainer = Brown100,
    secondary = Gold500,
    onSecondary = InkBlack,
    background = InkBlack,
    onBackground = PaperWhite,
    surface = InkBlack,
    onSurface = PaperWhite,
)

@Composable
fun ShufaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

**Step 6: Commit**

```bash
git add app/src/main/res/ app/src/main/java/com/example/shufa/ui/theme/
git commit -m "feat: add M3 theme and resource files"
```

---

## Task 4: 创建数据模型与仓库

**Files:**
- Create: `app/src/main/java/com/example/shufa/model/CalligraphyPost.kt`
- Create: `app/src/main/java/com/example/shufa/data/PostRepository.kt`
- Create: `app/src/main/assets/posts.json`

**Step 1: 创建 CalligraphyPost.kt**

```kotlin
package com.example.shufa.model

data class CalligraphyPost(
    val id: String,
    val title: String,
    val author: String,
    val dynasty: String,
    val style: CalligraphyStyle,
    val description: String,
    val imageUrl: String
)

enum class CalligraphyStyle(val label: String) {
    KAISHU("楷书"),
    XINGSHU("行书"),
    CAOSHU("草书"),
    LISHU("隶书"),
    ZHUANSHU("篆书")
}
```

**Step 2: 创建 PostRepository.kt**

```kotlin
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
            CalligraphyPost(
                id = obj.getString("id"),
                title = obj.getString("title"),
                author = obj.getString("author"),
                dynasty = obj.getString("dynasty"),
                style = CalligraphyStyle.valueOf(obj.getString("style")),
                description = obj.getString("description"),
                imageUrl = obj.getString("imageUrl")
            )
        }
    }
}
```

**Step 3: 创建 posts.json**

```json
[
    {
        "id": "yan-qin-li-biao",
        "title": "颜勤礼碑",
        "author": "颜真卿",
        "dynasty": "唐",
        "style": "KAISHU",
        "description": "颜真卿晚年楷书代表作，结构方正茂密，笔画横轻竖重。",
        "imageUrl": ""
    },
    {
        "id": "lan-ting-xu",
        "title": "兰亭集序",
        "author": "王羲之",
        "dynasty": "东晋",
        "style": "XINGSHU",
        "description": "天下第一行书，笔法精妙，气韵生动。",
        "imageUrl": ""
    },
    {
        "id": "zi-xi-tie",
        "title": "自叙帖",
        "author": "怀素",
        "dynasty": "唐",
        "style": "CAOSHU",
        "description": "草书代表作，笔势纵横奔放，如骤雨旋风。",
        "imageUrl": ""
    },
    {
        "id": "li-qi-bei",
        "title": "礼器碑",
        "author": "佚名",
        "dynasty": "东汉",
        "style": "LISHU",
        "description": "隶书经典碑刻，结体端庄，笔画瘦劲。",
        "imageUrl": ""
    },
    {
        "id": "yi-shan-bei",
        "title": "峄山碑",
        "author": "李斯",
        "dynasty": "秦",
        "style": "ZHUANSHU",
        "description": "小篆代表作，线条匀称，结构严谨。",
        "imageUrl": ""
    }
]
```

**Step 4: Commit**

```bash
git add app/src/main/java/com/example/shufa/model/ \
        app/src/main/java/com/example/shufa/data/ \
        app/src/main/assets/
git commit -m "feat: add data model, repository, and sample posts"
```

---

## Task 5: 创建导航图

**Files:**
- Create: `app/src/main/java/com/example/shufa/navigation/NavGraph.kt`

**Step 1: 创建 NavGraph.kt**

```kotlin
package com.example.shufa.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.shufa.ui.select.SelectScreen
import com.example.shufa.ui.view.ViewScreen

object Routes {
    const val SELECT = "select"
    const val VIEW = "view/{postId}"

    fun view(postId: String) = "view/$postId"
}

@Composable
fun ShufaNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.SELECT
    ) {
        composable(Routes.SELECT) {
            SelectScreen(
                onPostClick = { postId ->
                    navController.navigate(Routes.view(postId))
                }
            )
        }

        composable(
            route = Routes.VIEW,
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
            ViewScreen(
                postId = postId,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/com/example/shufa/navigation/
git commit -m "feat: add navigation graph with select and view routes"
```

---

## Task 6: 创建 MainActivity

**Files:**
- Create: `app/src/main/java/com/example/shufa/MainActivity.kt`

**Step 1: 创建 MainActivity.kt**

```kotlin
package com.example.shufa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.shufa.navigation.ShufaNavGraph
import com.example.shufa.ui.theme.ShufaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShufaTheme {
                val navController = rememberNavController()
                ShufaNavGraph(navController = navController)
            }
        }
    }
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/com/example/shufa/MainActivity.kt
git commit -m "feat: add MainActivity with Compose setup"
```

---

## Task 7: 创建选贴界面

**Files:**
- Create: `app/src/main/java/com/example/shufa/ui/select/SelectViewModel.kt`
- Create: `app/src/main/java/com/example/shufa/ui/select/SelectScreen.kt`

**Step 1: 创建 SelectViewModel.kt**

```kotlin
package com.example.shufa.ui.select

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shufa.data.PostRepository
import com.example.shufa.model.CalligraphyPost
import com.example.shufa.model.CalligraphyStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SelectUiState(
    val posts: List<CalligraphyPost> = emptyList(),
    val selectedStyle: CalligraphyStyle? = null,
    val isLoading: Boolean = true
)

class SelectViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PostRepository(application)

    private val _uiState = MutableStateFlow(SelectUiState())
    val uiState: StateFlow<SelectUiState> = _uiState.asStateFlow()

    init {
        loadPosts()
    }

    private fun loadPosts() {
        viewModelScope.launch {
            val posts = repository.getPosts()
            _uiState.value = SelectUiState(
                posts = posts,
                isLoading = false
            )
        }
    }

    fun filterByStyle(style: CalligraphyStyle?) {
        _uiState.value = _uiState.value.copy(selectedStyle = style)
    }

    fun getFilteredPosts(): List<CalligraphyPost> {
        val state = _uiState.value
        return if (state.selectedStyle != null) {
            state.posts.filter { it.style == state.selectedStyle }
        } else {
            state.posts
        }
    }
}
```

**Step 2: 创建 SelectScreen.kt**

```kotlin
package com.example.shufa.ui.select

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shufa.model.CalligraphyPost
import com.example.shufa.model.CalligraphyStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectScreen(
    onPostClick: (String) -> Unit,
    viewModel: SelectViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选贴") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            StyleFilterChips(
                selectedStyle = uiState.selectedStyle,
                onStyleSelected = { viewModel.filterByStyle(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(viewModel.getFilteredPosts()) { post ->
                    PostCard(
                        post = post,
                        onClick = { onPostClick(post.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StyleFilterChips(
    selectedStyle: CalligraphyStyle?,
    onStyleSelected: (CalligraphyStyle?) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedStyle == null,
            onClick = { onStyleSelected(null) },
            label = { Text("全部") }
        )
        CalligraphyStyle.entries.forEach { style ->
            FilterChip(
                selected = selectedStyle == style,
                onClick = { onStyleSelected(style) },
                label = { Text(style.label) }
            )
        }
    }
}

@Composable
private fun PostCard(
    post: CalligraphyPost,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${post.author} · ${post.dynasty}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = post.style.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
```

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/shufa/ui/select/
git commit -m "feat: add select screen with style filter chips and grid"
```

---

## Task 8: 创建看贴界面

**Files:**
- Create: `app/src/main/java/com/example/shufa/ui/view/ViewViewModel.kt`
- Create: `app/src/main/java/com/example/shufa/ui/view/ViewScreen.kt`

**Step 1: 创建 ViewViewModel.kt**

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
            val post = repository.getPostById(postId)
            _uiState.value = ViewUiState(
                post = post,
                isLoading = false
            )
        }
    }
}
```

**Step 2: 创建 ViewScreen.kt**

```kotlin
package com.example.shufa.ui.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewScreen(
    postId: String,
    onBackClick: () -> Unit,
    viewModel: ViewViewModel = viewModel()
) {
    LaunchedEffect(postId) {
        viewModel.loadPost(postId)
    }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.post?.title ?: "看贴") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("加载中...")
                }
            }
            uiState.post != null -> {
                val post = uiState.post!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${post.author} · ${post.dynasty} · ${post.style.label}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = post.description,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("字帖未找到")
                }
            }
        }
    }
}
```

**Step 3: Commit**

```bash
git add app/src/main/java/com/example/shufa/ui/view/
git commit -m "feat: add view screen with post detail display"
```

---

## Task 9: 验证构建

**Step 1: 运行 Gradle 构建**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

**Step 2: 更新 AGENTS.md**

补充验证通过的构建命令。

**Step 3: Commit**

```bash
git add AGENTS.md
git commit -m "docs: update AGENTS.md with verified build commands"
```

---

## Task 10: 最终检查

**Step 1: 运行 lint 检查**

```bash
./gradlew lintDebug
```

**Step 2: 确认无编译错误和严重 lint 问题**

**Step 3: 最终 commit（如有修复）**
