# 整帖与单字查看实现计划

> **For OpenCode:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现真实的整帖图片查看和单字查看功能，支持点击放大

**Architecture:** 使用 Coil 加载网络图片，数据模型扩展支持图片URL和单字数据，点击单字弹出全屏放大对话框

**Tech Stack:** Coil (图片加载), Material 3 Dialog, Modifier.graphicsLayer (缩放)

---

## Task 1: 添加 Coil 依赖

**Files:**
- Modify: `app/build.gradle.kts`

**Step 1: 添加 Coil 依赖**

在 dependencies 块中添加：
```kotlin
// Coil for image loading
implementation("io.coil-kt:coil-compose:2.7.0")
```

**Step 2: Commit**

```bash
git add app/build.gradle.kts
git commit -m "deps: add Coil for image loading"
```

---

## Task 2: 扩展数据模型

**Files:**
- Modify: `app/src/main/java/com/example/shufa/model/CalligraphyPost.kt`
- Modify: `app/src/main/assets/posts.json`

**Step 1: 更新 CalligraphyPost 模型**

```kotlin
package com.example.shufa.model

data class CalligraphyPost(
    val id: String,
    val title: String,
    val author: String,
    val dynasty: String,
    val style: CalligraphyStyle,
    val description: String,
    val imageUrl: String,
    val characters: List<String> = emptyList()
)

enum class CalligraphyStyle(val label: String) {
    KAISHU("楷书"),
    XINGSHU("行书"),
    CAOSHU("草书"),
    LISHU("隶书"),
    ZHUANSHU("篆书")
}
```

**Step 2: 更新 posts.json 添加图片URL和单字数据**

为每个帖子添加 `imageUrl` 和 `characters` 字段。使用占位图URL，后续替换为真实图片。

```json
[
    {
        "id": "yan-qin-li-biao",
        "title": "颜勤礼碑",
        "author": "颜真卿",
        "dynasty": "唐",
        "style": "KAISHU",
        "description": "颜真卿晚年楷书代表作，结构方正茂密，笔画横轻竖重。",
        "imageUrl": "https://upload.wikimedia.org/wikipedia/commons/thumb/6/65/Yan_Zhenqing_-_Yan_Qinli_Bei.jpg/800px-Yan_Zhenqing_-_Yan_Qinli_Bei.jpg",
        "characters": ["颜", "勤", "礼", "碑", "唐", "故", "秘", "书", "省", "著", "作", "郎", "兼", "侍", "御", "史"]
    },
    {
        "id": "lan-ting-xu",
        "title": "兰亭集序",
        "author": "王羲之",
        "dynasty": "东晋",
        "style": "XINGSHU",
        "description": "天下第一行书，笔法精妙，气韵生动。",
        "imageUrl": "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a8/LantingXu.jpg/800px-LantingXu.jpg",
        "characters": ["永", "和", "九", "年", "岁", "在", "癸", "丑", "暮", "春", "之", "初", "会", "于", "会", "稽"]
    },
    {
        "id": "zi-xi-tie",
        "title": "自叙帖",
        "author": "怀素",
        "dynasty": "唐",
        "style": "CAOSHU",
        "description": "草书代表作，笔势纵横奔放，如骤雨旋风。",
        "imageUrl": "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5c/Huaisu_-_Autobiography.jpg/800px-Huaisu_-_Autobiography.jpg",
        "characters": ["自", "叙", "帖", "怀", "素", "家", "本", "沙", "门", "幼", "而", "事", "佛", "经", "禅", "之"]
    },
    {
        "id": "li-qi-bei",
        "title": "礼器碑",
        "author": "佚名",
        "dynasty": "东汉",
        "style": "LISHU",
        "description": "隶书经典碑刻，结体端庄，笔画瘦劲。",
        "imageUrl": "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4d/Liqi_Bei.jpg/800px-Liqi_Bei.jpg",
        "characters": ["礼", "器", "碑", "汉", "鲁", "相", "韩", "敕", "造", "孔", "子", "庙", "碑", "阴", "额", "篆"]
    },
    {
        "id": "yi-shan-bei",
        "title": "峄山碑",
        "author": "李斯",
        "dynasty": "秦",
        "style": "ZHUANSHU",
        "description": "小篆代表作，线条匀称，结构严谨。",
        "imageUrl": "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8f/Yishan_Bei.jpg/800px-Yishan_Bei.jpg",
        "characters": ["峄", "山", "碑", "秦", "始", "皇", "帝", "立", "石", "刻", "辞", "明", "德", "圣", "功", "量"]
    }
]
```

**Step 3: 更新 PostRepository 解析新字段**

```kotlin
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
```

**Step 4: Commit**

```bash
git add app/src/main/java/com/example/shufa/model/CalligraphyPost.kt \
        app/src/main/java/com/example/shufa/data/PostRepository.kt \
        app/src/main/assets/posts.json
git commit -m "feat: extend data model with image URL and character list"
```

---

## Task 3: 实现整帖图片查看

**Files:**
- Modify: `app/src/main/java/com/example/shufa/ui/view/ViewScreen.kt`

**Step 1: 更新 FullPieceView 使用 Coil 加载图片**

```kotlin
@Composable
private fun FullPieceView(post: CalligraphyPost) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (post.imageUrl.isNotEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(post.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = post.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "（图片待添加）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/com/example/shufa/ui/view/ViewScreen.kt
git commit -m "feat: implement full piece view with Coil image loading"
```

---

## Task 4: 实现单字查看与点击放大

**Files:**
- Modify: `app/src/main/java/com/example/shufa/ui/view/ViewScreen.kt`

**Step 1: 更新 SingleCharacterView 使用真实单字数据**

```kotlin
@Composable
private fun SingleCharacterView(post: CalligraphyPost) {
    val characters = if (post.characters.isNotEmpty()) {
        post.characters
    } else {
        listOf("暂", "无", "单", "字", "数", "据")
    }

    var selectedCharacter by remember { mutableStateOf<String?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(characters) { character ->
            Card(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clickable { selectedCharacter = character },
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = character,
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    selectedCharacter?.let { character ->
        CharacterZoomDialog(
            character = character,
            onDismiss = { selectedCharacter = null }
        )
    }
}
```

**Step 2: 添加 CharacterZoomDialog**

```kotlin
@Composable
private fun CharacterZoomDialog(
    character: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = character,
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        }
    }
}
```

**Step 3: 添加必要的 import**

```kotlin
import androidx.compose.material3.Dialog
import androidx.compose.runtime.mutableStateOf
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
```

**Step 4: Commit**

```bash
git add app/src/main/java/com/example/shufa/ui/view/ViewScreen.kt
git commit -m "feat: implement single character view with zoom dialog"
```

---

## Task 5: 构建验证与测试

**Step 1: 构建项目**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

**Step 2: 安装并测试**

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.example.shufa/.MainActivity
```

**Step 3: 测试要点**
- 选贴页面显示帖子卡片
- 点击帖子进入详情页
- 整帖查看：显示图片（或占位符）
- 单字查看：显示单字网格
- 点击单字：弹出放大对话框
- 返回按钮正常工作

**Step 4: 最终 commit（如有修复）**

---

## 注意事项

1. 图片URL使用维基百科占位图，后续需替换为真实碑帖图片
2. 单字数据为示例数据，后续需根据真实碑帖内容更新
3. Coil 会自动处理图片缓存和加载状态
4. 放大对话框使用 Material 3 Dialog，支持点击外部关闭
