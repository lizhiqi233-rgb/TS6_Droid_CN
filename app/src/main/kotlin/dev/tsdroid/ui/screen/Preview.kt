package dev.tsdroid.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.tsdroid.ui.theme.TsDroidTheme

@Preview(name = "Chat Panel", showBackground = true, widthDp = 360, heightDp = 600)
@Composable
private fun PreviewChatPanel() {
    TsDroidTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TabRow(
                        selectedTabIndex = 0,
                        modifier = Modifier.weight(1f),
                    ) {
                        Tab(selected = true, onClick = {}, text = { Text("频道") })
                        Tab(selected = false, onClick = {}, text = { Text("私信") })
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Close, "关闭")
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(selected = true, onClick = {}, label = { Text("全部") })
                    FilterChip(selected = false, onClick = {}, label = { Text("用户A") })
                }

                Spacer(Modifier.height(8.dp))

                val messages = listOf(
                    "你好！欢迎来到频道" to true,
                    "大家好，这是一条测试消息" to false,
                    "毛玻璃效果看起来不错" to true,
                    "背景图片也能看到" to false,
                )
                messages.forEach { (text, isMe) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = if (isMe) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Text(
                                text = text,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    placeholder = { Text("频道消息…") },
                )
            }
        }
    }
}

@Preview(name = "Settings Dialog", showBackground = true, widthDp = 360, heightDp = 400)
@Composable
private fun PreviewSettingsDialog() {
    TsDroidTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("设置", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(16.dp))

                    Text("音量增益 : ×1.5", style = MaterialTheme.typography.bodyLarge)
                    Slider(value = 1.5f, onValueChange = {}, valueRange = 1.0f..8.0f, steps = 13)
                    Spacer(Modifier.height(8.dp))

                    listOf(
                        "显示视频链接缩略图" to false,
                        "自动加载图片" to true,
                        "启用悬浮窗" to false,
                        "我是二刺螈" to true,
                        "麦克风降噪" to true,
                    ).forEach { (label, checked) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(label, modifier = Modifier.weight(1f))
                            Switch(checked = checked, onCheckedChange = {})
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Bottom Navigation", showBackground = true, widthDp = 360, heightDp = 80)
@Composable
private fun PreviewBottomNav() {
    TsDroidTheme {
        NavigationBar {
            NavigationBarItem(
                selected = true,
                onClick = {},
                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                label = { Text("主页") },
            )
            NavigationBarItem(
                selected = false,
                onClick = {},
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                label = { Text("设置") },
            )
        }
    }
}

@Preview(name = "Bookmark Card", showBackground = true, widthDp = 360, heightDp = 100)
@Composable
private fun PreviewBookmarkCard() {
    TsDroidTheme {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("yuaxi.cn", fontWeight = FontWeight.Medium)
                    Text("yuaxi.cn:9987", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalButton(onClick = {}) {
                    Text("连接")
                }
            }
        }
    }
}
