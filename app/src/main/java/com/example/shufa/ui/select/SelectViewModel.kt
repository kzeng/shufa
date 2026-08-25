package com.example.shufa.ui.select

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shufa.data.PostRepository
import com.example.shufa.model.CalligraphyPost
import com.example.shufa.model.CalligraphyStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class SelectUiState(
    val posts: List<CalligraphyPost> = emptyList(),
    val selectedStyle: CalligraphyStyle? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val networkResults: List<CalligraphyPost> = emptyList(),
    val isSearching: Boolean = false
)

class SelectViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PostRepository(application)
    private val prefs = application.getSharedPreferences("shufa_prefs", 0)

    private val _uiState = MutableStateFlow(SelectUiState())
    val uiState: StateFlow<SelectUiState> = _uiState.asStateFlow()

    private val _selectedStyle = MutableStateFlow<CalligraphyStyle?>(CalligraphyStyle.LISHU)
    private val _searchQuery = MutableStateFlow("")

    init {
        loadPosts()
    }

    private fun loadPosts() {
        viewModelScope.launch {
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
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.length >= 2) {
            searchNetwork(query)
        }
    }

    private fun searchNetwork(query: String) {
        _uiState.value = _uiState.value.copy(isSearching = true)
        viewModelScope.launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    fetchSearchResults(query)
                }
                val localIds = _uiState.value.posts.map { it.id }.toSet()
                val filtered = results.filter { it.id !in localIds }
                _uiState.value = _uiState.value.copy(
                    networkResults = filtered,
                    isSearching = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    networkResults = emptyList(),
                    isSearching = false
                )
            }
        }
    }

    private fun fetchSearchResults(query: String): List<CalligraphyPost> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = URL("https://www.zitiewang.com/search.php?keyword=$encoded")
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
        conn.connectTimeout = 10000
        conn.readTimeout = 10000

        return try {
            val html = conn.inputStream.bufferedReader().use { it.readText() }
            parseSearchResults(html)
        } catch (e: Exception) {
            emptyList()
        } finally {
            conn.disconnect()
        }
    }

    private fun parseSearchResults(html: String): List<CalligraphyPost> {
        val results = mutableListOf<CalligraphyPost>()
        val linkPattern = Regex("""<a[^>]+href=["']([^"']*shufa/[^"']*\.htm)["'][^>]*>([^<]+)</a>""", RegexOption.DOT_MATCHES_ALL)
        val imgPattern = Regex("""src=["'](https?://img\.zitiewang\.com/file/[^"']+)["']""")
        val descPattern = Regex("""<p[^>]*class=["'][^"']*desc[^"']*["'][^>]*>([^<]+)</p>""")

        val matches = linkPattern.findAll(html)
        val seen = mutableSetOf<String>()

        for (match in matches) {
            val path = match.groupValues[1]
            val title = match.groupValues[2].trim()
            if (title.length < 2 || title.length > 20 || path in seen) continue
            seen.add(path)

            val id = path.removeSuffix(".htm")
                .replace("shufa/", "")
                .replace("/", "-")
                .lowercase()

            val imageUrl = imgPattern.find(html)?.groupValues?.get(1) ?: ""
            val desc = descPattern.find(html)?.groupValues?.get(1)?.trim() ?: ""

            val style = guessStyle(title)
            results.add(
                CalligraphyPost(
                    id = id,
                    title = title,
                    author = extractAuthor(title),
                    dynasty = extractDynasty(title),
                    style = style,
                    description = desc.ifEmpty { "$title，${style.label}碑帖。" },
                    imageUrls = if (imageUrl.isNotEmpty()) listOf(imageUrl) else emptyList(),
                    characters = extractCharsFromTitle(title)
                )
            )
            if (results.size >= 20) break
        }
        return results
    }

    private fun guessStyle(title: String): CalligraphyStyle {
        return when {
            title.contains("楷") || title.contains("多宝塔") || title.contains("九成宫") ||
            title.contains("玄秘塔") || title.contains("麻姑") || title.contains("颜家庙") -> CalligraphyStyle.KAISHU
            title.contains("行") || title.contains("兰亭") || title.contains("祭侄") ||
            title.contains("寒食") || title.contains("蜀素") || title.contains("苕溪") -> CalligraphyStyle.XINGSHU
            title.contains("草") || title.contains("自叙") || title.contains("书谱") ||
            title.contains("千字文") && title.contains("草") -> CalligraphyStyle.CAOSHU
            title.contains("隶") || title.contains("曹全") || title.contains("张迁") ||
            title.contains("礼器") || title.contains("乙瑛") || title.contains("史晨") ||
            title.contains("石门") -> CalligraphyStyle.LISHU
            title.contains("篆") || title.contains("峄山") || title.contains("散氏") ||
            title.contains("毛公") || title.contains("泰山刻石") -> CalligraphyStyle.ZHUANSHU
            else -> CalligraphyStyle.KAISHU
        }
    }

    private fun extractAuthor(title: String): String {
        val knownAuthors = mapOf(
            "颜真卿" to listOf("颜勤礼", "多宝塔", "麻姑", "颜家庙", "颜氏家庙"),
            "王羲之" to listOf("兰亭", "快雪", "丧乱", "姨母", "初月"),
            "柳公权" to listOf("玄秘塔", "神策军", "金刚经"),
            "欧阳询" to listOf("九成宫", "化度寺", "皇甫诞"),
            "褚遂良" to listOf("雁塔圣教", "倪宽赞", "孟法师"),
            "怀素" to listOf("自叙", "苦笋", "论书"),
            "张旭" to listOf("古诗四帖", "肚痛"),
            "赵孟頫" to listOf("洛神赋", "胆巴碑", "前后赤壁"),
            "米芾" to listOf("蜀素", "苕溪", "研山"),
            "苏轼" to listOf("寒食", "赤壁"),
            "黄庭坚" to listOf("松风阁", "诸上座"),
            "蔡襄" to listOf("自书诗", "万安桥"),
            "李斯" to listOf("峄山", "泰山刻石", "琅琊台"),
            "孙过庭" to listOf("书谱"),
            "钟繇" to listOf("宣示表", "荐季直表", "贺捷表"),
            "王献之" to listOf("洛神赋十三行", "鸭头丸"),
            "智永" to listOf("真草千字文"),
            "褚遂良" to listOf("雁塔圣教序"),
            "蔡邕" to listOf("熹平石经"),
            "史晨" to listOf("史晨碑"),
            "曹全" to listOf("曹全碑"),
            "张迁" to listOf("张迁碑")
        )
        for ((author, keywords) in knownAuthors) {
            if (keywords.any { title.contains(it) }) return author
        }
        return "佚名"
    }

    private fun extractDynasty(title: String): String {
        return when {
            title.contains("秦") || title.contains("峄山") || title.contains("泰山刻石") || title.contains("琅琊") -> "秦"
            title.contains("汉") || title.contains("曹全") || title.contains("张迁") ||
            title.contains("礼器") || title.contains("乙瑛") || title.contains("史晨") ||
            title.contains("石门") || title.contains("鲜于璜") || title.contains("夏承") -> "东汉"
            title.contains("魏") || title.contains("晋") || title.contains("宣示") ||
            title.contains("贺捷") || title.contains("兰亭") || title.contains("快雪") -> "东晋"
            title.contains("隋") || title.contains("智永") -> "隋"
            title.contains("唐") || title.contains("颜") || title.contains("柳") ||
            title.contains("欧阳") || title.contains("褚") || title.contains("怀素") ||
            title.contains("张旭") || title.contains("多宝塔") || title.contains("九成宫") ||
            title.contains("玄秘塔") || title.contains("自叙") -> "唐"
            title.contains("宋") || title.contains("苏轼") || title.contains("米芾") ||
            title.contains("黄庭坚") || title.contains("蔡襄") || title.contains("寒食") ||
            title.contains("蜀素") || title.contains("苕溪") -> "宋"
            title.contains("元") || title.contains("赵孟頫") -> "元"
            title.contains("明") || title.contains("董其昌") -> "明"
            title.contains("清") || title.contains("邓石如") || title.contains("伊秉绶") -> "清"
            else -> "佚名"
        }
    }

    private fun extractCharsFromTitle(title: String): List<String> {
        val chars = title.replace(Regex("[·\\s]"), "").toList().map { it.toString() }
        return chars.take(8)
    }

    fun addNetworkPost(post: CalligraphyPost) {
        val current = _uiState.value
        val updated = current.posts + post
        _uiState.value = current.copy(
            posts = updated,
            networkResults = current.networkResults.filter { it.id != post.id }
        )
        viewModelScope.launch {
            repository.addPost(post)
        }
    }

    fun clearNetworkResults() {
        _uiState.value = _uiState.value.copy(networkResults = emptyList())
    }
}
