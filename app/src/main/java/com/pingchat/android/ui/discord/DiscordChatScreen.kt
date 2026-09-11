package com.pingchat.android.ui.discord

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pingchat.android.ui.ChatViewModel
import com.pingchat.android.ui.theme.PingchatFontFamily
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DiscordChatScreen(viewModel: ChatViewModel) {
    val context = LocalContext.current
    val accent by ColorOptionsStore.accent.collectAsStateWithLifecycle()
    val groups = rememberGroups(viewModel)
    var selectedGroup by remember { mutableStateOf<PingGroup?>(groups.firstOrNull()) }
    var selectedChannel by remember { mutableStateOf<String?>("general") }
    var messageText by remember { mutableStateOf(TextFieldValue("")) }
    var showUsernameDialog by remember { mutableStateOf(!UsernameManager.isUsernameSet(context)) }
    var showColorPicker by remember { mutableStateOf(false) }
    val nickname by viewModel.nickname.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val channelMessages by viewModel.channelMessages.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Groups from joinedChannels + location
    LaunchedEffect(groups) {
        if (selectedGroup == null && groups.isNotEmpty()) selectedGroup = groups.first()
    }

    // Username dialog
    if (showUsernameDialog) {
        var input by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Choose your username", fontWeight = FontWeight.Bold, fontFamily = PingchatFontFamily) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("This is how others see you. Keep it simple - you can change anytime in settings.", color = DiscordPalette.Grey400, fontSize = 12.sp)
                    OutlinedTextField(value = input, onValueChange = { input = it }, label = { Text("Username") }, singleLine = true, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (input.trim().length >=2) {
                        UsernameManager.setUsername(context, input.trim())
                        viewModel.setNickname(input.trim())
                        showUsernameDialog = false
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = accent.color)) { Text("Continue") }
            }
        )
    }

    // Color picker dialog
    if (showColorPicker) {
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text("Choose your color", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Pick your accent - like Discord themes. Changes bubbles & highlights instantly.", color = DiscordPalette.Grey400, fontSize = 12.sp)
                    // Use entries for Kotlin 1.9+ and handle composable loop correctly
                    val accentList = AccentColor.entries
                    for (chunk in accentList.chunked(3)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            for (ac in chunk) {
                                val selected = ac == accent
                                Button(
                                    onClick = { ColorOptionsStore.setAccent(context, ac) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = ac.color, contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(8.dp)
                                ) {
                                    Text(ac.displayName, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showColorPicker = false }) { Text("Done") } }
        )
    }

    DiscordShell(
        groups = groups,
        selectedGroup = selectedGroup,
        onSelectGroup = { selectedGroup = it; selectedChannel = "general" },
        onCreateGroup = { name -> viewModel.joinChannel(name, ""); selectedGroup = PingGroup(name,name) },
        onJoinGroup = { name -> viewModel.joinChannel(name, ""); selectedGroup = PingGroup(name,name) },
        accent = accent.color,
        channelContent = {
            SimpleChannelList(
                channels = listOf("general", "random", "help", "mesh") + viewModel.joinedChannels.collectAsStateWithLifecycle().value.toList(),
                selected = selectedChannel,
                onSelect = { selectedChannel = it; if (it != "general" && it != "random") viewModel.switchToChannel(it) else viewModel.switchToChannel(null) },
                accent = accent.color
            )
        },
        chatContent = {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().background(DiscordPalette.DarkBackground).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("# ${selectedChannel ?: "general"}", color = DiscordPalette.Grey100, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = PingchatFontFamily)
                    Text("${selectedGroup?.name ?: "PingChat"} • ${messages.size} messages", color = DiscordPalette.Grey400, fontSize = 11.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { showColorPicker = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Color options", tint = DiscordPalette.Grey400)
                    }
                }
            }
            HorizontalDivider(color = Color.White.copy(0.08f))
            // Messages with 3D bubbles
            val displayMessages by remember(messages, channelMessages, selectedChannel) {
                mutableStateOf(
                    when {
                        selectedChannel != null && selectedChannel != "general" && selectedChannel != "random" -> channelMessages[selectedChannel] ?: emptyList()
                        else -> messages
                    }
                )
            }
            LazyColumn(state = listState, modifier = Modifier.weight(1f).background(DiscordPalette.DarkBackground), contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(displayMessages) { msg ->
                    val isOwn = msg.sender == nickname
                    val time = try { SimpleDateFormat("HH:mm", Locale.getDefault()).format(msg.timestamp) } catch (_: Exception) { "" }
                    DiscordMessageBubble(
                        sender = msg.sender,
                        time = time,
                        message = msg.content,
                        isOwn = isOwn,
                        accent = accent.color
                    )
                }
            }
            // Input - Discord style
            Row(
                modifier = Modifier.fillMaxWidth().background(DiscordPalette.DarkBackground).padding(12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(DiscordPalette.DarkInput).padding(4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Add, contentDescription = "Attach", tint = DiscordPalette.Grey400, modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.White.copy(0.12f)).padding(4.dp))
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = messageText.text,
                            onValueChange = { messageText = TextFieldValue(it); viewModel.setConversationDraft(null, it); viewModel.updateCommandSuggestions(it); viewModel.updateMentionSuggestions(it) },
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            textStyle = LocalTextStyle.current.copy(color = DiscordPalette.Grey100, fontSize = 14.sp, fontFamily = PingchatFontFamily),
                            decorationBox = { inner ->
                                if (messageText.text.isEmpty()) Text("Message #${selectedChannel ?: "general"}", color = DiscordPalette.Grey400, fontSize = 14.sp, fontFamily = PingchatFontFamily)
                                inner()
                            }
                        )
                        Button(onClick = {
                            if (messageText.text.isNotBlank()) {
                                viewModel.sendMessage(messageText.text.trim()) { ok -> if (ok) messageText = TextFieldValue("") }
                            }
                        }, colors = ButtonDefaults.buttonColors(containerColor = accent.color), shape = RoundedCornerShape(6.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("Send", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun rememberGroups(viewModel: ChatViewModel): List<PingGroup> {
    val joined by viewModel.joinedChannels.collectAsStateWithLifecycle()
    val peers by viewModel.connectedPeers.collectAsStateWithLifecycle()
    return remember(joined, peers) {
        val list = mutableListOf<PingGroup>()
        list.add(PingGroup("home","Home"))
        joined.forEachIndexed { idx, ch -> list.add(PingGroup(ch, ch, "#", unread = 0)) }
        if (peers.isNotEmpty()) list.add(PingGroup("mesh","Mesh (${peers.size})","◈"))
        if (list.size == 1) list.add(PingGroup("welcome","Welcome","W"))
        list
    }
}
