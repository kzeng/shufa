package com.example.shufa.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.shufa.BuildConfig
import com.example.shufa.R
import com.example.shufa.ui.favorites.FavoritesContent
import com.example.shufa.ui.select.SelectContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onPostClick: (String) -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

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
                        Spacer(modifier = Modifier.width(12.dp))
                        HomeTab(
                            text = "选贴",
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        HomeTab(
                            text = "收藏",
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 }
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (darkTheme) "切换到浅色模式" else "切换到深色模式",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> SelectContent(onPostClick = onPostClick)
                else -> FavoritesContent(onPostClick = onPostClick)
            }
        }
    }

    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false },
            onPrivacyPolicyClick = { showPrivacyPolicyDialog = true }
        )
    }

    if (showPrivacyPolicyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyPolicyDialog = false })
    }
}

@Composable
private fun HomeTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = MaterialTheme.colorScheme.onPrimaryContainer
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .width(64.dp)
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else color.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.size(6.dp))
        Box(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(2.dp)
                )
        )
    }
}

@Composable
private fun AboutDialog(
    onDismiss: () -> Unit,
    onPrivacyPolicyClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "关于",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_app_logo),
                    contentDescription = "书法学习 Logo",
                    modifier = Modifier.size(96.dp)
                )
                Text("书法学习", textAlign = TextAlign.Center)
                Text("作者：ZengKai", textAlign = TextAlign.Center)
                Text("邮箱：zengkai001@gmail.com", textAlign = TextAlign.Center)
                Text("版本：${BuildConfig.VERSION_NAME}", textAlign = TextAlign.Center)
                TextButton(onClick = onPrivacyPolicyClick) {
                    Text("隐私政策")
                }
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
private fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "隐私政策",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("生效日期：2026 年 9 月 7 日")
                Text("书法学习（以下简称“本应用”）尊重并保护用户隐私。本应用不要求注册账号，不包含广告、支付、位置、通讯录或相机等功能。")
                Text("本地数据：收藏的字帖、显示主题、字体大小和用户添加的字帖保存在设备本地。本应用不会将这些本地数据上传到自有服务器。用户可以通过 Android 系统设置清除本应用数据。")
                Text("网络访问：当用户使用字帖搜索功能时，搜索关键词会发送至 zitiewang.com 以获取搜索结果。本应用还会从字帖数据中记录的第三方网站加载字帖图片。相关网站可能按照各自的隐私政策处理网络请求信息，例如 IP 地址和请求时间。")
                Text("第三方内容：本应用展示的部分字帖文字、说明和图片来自第三方网站。本应用不使用第三方网站的账号登录或支付服务。")
                Text("数据安全：本应用不建立自有用户账号体系，也不出售用户数据。用户应避免在搜索框中输入姓名、联系方式或其他不必要的个人信息。")
                Text("联系我们：如对隐私或数据处理有疑问，请联系 zengkai001@gmail.com。")
                Text("本政策可能因功能或法律要求变化而更新。更新后的版本会在本应用内显示新的生效日期。")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
