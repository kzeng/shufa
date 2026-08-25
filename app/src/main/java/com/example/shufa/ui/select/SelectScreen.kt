package com.example.shufa.ui.select

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.shufa.R
import com.example.shufa.model.CalligraphyPost
import com.example.shufa.model.CalligraphyStyle
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectScreen(
    onPostClick: (String) -> Unit,
    viewModel: SelectViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var currentPage by remember { mutableIntStateOf(0) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val isSearching = uiState.searchQuery.isNotEmpty()
    val filteredPosts = uiState.posts.filter { post ->
        val matchesSearch = uiState.searchQuery.isEmpty() ||
            post.title.contains(uiState.searchQuery, ignoreCase = true) ||
            post.author.contains(uiState.searchQuery, ignoreCase = true) ||
            post.dynasty.contains(uiState.searchQuery, ignoreCase = true) ||
            post.style.label.contains(uiState.searchQuery, ignoreCase = true) ||
            post.description.contains(uiState.searchQuery, ignoreCase = true)
        // 搜索时忽略风格筛选（与 ViewModel 的 DB 查询行为一致）
        val matchesStyle = isSearching ||
            uiState.selectedStyle == null ||
            post.style == uiState.selectedStyle
        matchesStyle && matchesSearch
    }

    val totalPages = (filteredPosts.size + PAGE_SIZE - 1) / PAGE_SIZE
    val pagedPosts = filteredPosts.drop(currentPage * PAGE_SIZE).take(PAGE_SIZE)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_app_logo),
                            contentDescription = "App 图标",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { showAboutDialog = true }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("选贴")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = {
                    viewModel.updateSearchQuery(it)
                    currentPage = 0
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("搜索贴名、作者、年代...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索"
                    )
                },
                singleLine = true
            )

            StyleFilterChips(
                selectedStyle = uiState.selectedStyle,
                onStyleSelected = {
                    viewModel.filterByStyle(it)
                    currentPage = 0
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(pagedPosts) { post ->
                    PostCard(
                        post = post,
                        onClick = { onPostClick(post.id) }
                    )
                }

                if (uiState.searchQuery.length >= 2 && uiState.isSearching) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("正在网络搜索...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                if (uiState.networkResults.isNotEmpty()) {
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = "网络搜索结果（点击+添加到本地）",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(uiState.networkResults) { post ->
                        NetworkPostCard(
                            post = post,
                            onAdd = {
                                viewModel.addNetworkPost(post)
                                scope.launch {
                                    snackbarHostState.showSnackbar("已添加「${post.title}」到本地")
                                }
                            },
                            onClick = { onPostClick(post.id) }
                        )
                    }
                }

                if (uiState.searchQuery.length >= 2 && !uiState.isSearching &&
                    filteredPosts.isEmpty() && uiState.networkResults.isEmpty()) {
                    item {
                        Text(
                            text = "未找到相关碑帖",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (totalPages > 1) {
                PaginationBar(
                    currentPage = currentPage,
                    totalPages = totalPages,
                    onPageSelected = { currentPage = it }
                )
            }
        }
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关于") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("APP：书法学习")
                Text("Author: Zengkai001@qq.com")
                Text("Version: 0.0.3")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}

@Composable
private fun PaginationBar(
    currentPage: Int,
    totalPages: Int,
    onPageSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (currentPage > 0) {
            Text(
                text = "上一页",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onPageSelected(currentPage - 1) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        val startPage = maxOf(0, currentPage - 2)
        val endPage = minOf(totalPages, startPage + 5)

        for (i in startPage until endPage) {
            Text(
                text = "${i + 1}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (i == currentPage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .background(
                        if (i == currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(4.dp)
                    )
                    .clickable { onPageSelected(i) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        if (currentPage < totalPages - 1) {
            Text(
                text = "下一页",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onPageSelected(currentPage + 1) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${currentPage + 1}/$totalPages",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StyleFilterChips(
    selectedStyle: CalligraphyStyle?,
    onStyleSelected: (CalligraphyStyle?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedStyle == null,
            onClick = { onStyleSelected(null) },
            label = { Text("全部") }
        )
        listOf(
            CalligraphyStyle.ZHUANSHU,
            CalligraphyStyle.LISHU,
            CalligraphyStyle.KAISHU,
            CalligraphyStyle.XINGSHU,
            CalligraphyStyle.CAOSHU
        ).forEach { style ->
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (post.imageUrls.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(post.imageUrls.first())
                        .crossfade(true)
                        .setHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                        .build(),
                    contentDescription = post.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(80.dp)
                        .height(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${post.author} · ${post.dynasty} · ${post.style.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = post.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NetworkPostCard(
    post: CalligraphyPost,
    onAdd: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (post.imageUrls.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(post.imageUrls.first())
                        .crossfade(true)
                        .setHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                        .build(),
                    contentDescription = post.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(60.dp)
                        .height(75.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onClick)
                )
            }

            Column(
                modifier = Modifier.weight(1f).clickable(onClick = onClick),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${post.author} · ${post.dynasty} · ${post.style.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = post.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "添加到本地",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(36.dp)
                    .clickable(onClick = onAdd)
                    .padding(6.dp)
            )
        }
    }
}
