package com.pingchat.android.ui.discord

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pingchat.android.R
import com.pingchat.android.ui.theme.PingchatFontFamily

// Simplified groups = Discord servers. One-tap create/join, no complicated flow.
data class PingGroup(val id: String, val name: String, val icon: String = "#", val unread: Int = 0)

@Composable
fun DiscordShell(
    groups: List<PingGroup>,
    selectedGroup: PingGroup?,
    onSelectGroup: (PingGroup) -> Unit,
    onCreateGroup: (String) -> Unit,
    onJoinGroup: (String) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    channelContent: @Composable () -> Unit,
    chatContent: @Composable ColumnScope.() -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    val context = LocalContext.current

    Row(modifier = modifier.fillMaxSize().background(DiscordPalette.DarkBackground)) {
        // Rail - servers
        Column(
            modifier = Modifier.width(72.dp).fillMaxHeight().background(DiscordPalette.DarkRail).padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Home (PingChat)
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(accent).clickable { onSelectGroup(PingGroup("home","Home")) },
                contentAlignment = Alignment.Center
            ) {
                Text("PC", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            HorizontalDivider(modifier = Modifier.width(32.dp).padding(vertical = 4.dp), color = Color.White.copy(0.1f))
            // Groups
            groups.forEach { g ->
                val selected = selectedGroup?.id == g.id
                Box(
                    modifier = Modifier.size(48.dp).clip(if (selected) RoundedCornerShape(16.dp) else CircleShape).background(if (selected) accent else DiscordPalette.DarkCard).clickable { onSelectGroup(g) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(g.name.take(2).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (g.unread > 0) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(DiscordPalette.Red).align(Alignment.TopEnd))
                    }
                }
            }
            // Add group
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(DiscordPalette.Green.copy(0.15f)).clickable { showCreateDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create group", tint = DiscordPalette.Green, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { /* settings handled outside */ }) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = DiscordPalette.Grey400)
            }
        }
        // Channel list
        Column(
            modifier = Modifier.width(240.dp).fillMaxHeight().background(DiscordPalette.DarkSidebar).padding(8.dp)
        ) {
            Text(selectedGroup?.name ?: "PingChat", color = DiscordPalette.Grey100, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = PingchatFontFamily, modifier = Modifier.padding(8.dp))
            HorizontalDivider(color = Color.White.copy(0.08f))
            Spacer(Modifier.height(8.dp))
            // Simplified channels: just show channelContent slot (caller provides channel list)
            Box(modifier = Modifier.weight(1f)) { channelContent() }
            // Simplified create/join
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Button(onClick = { showCreateDialog = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = accent), shape = RoundedCornerShape(4.dp)) {
                    Text("New Group", fontSize = 12.sp)
                }
            }
        }
        // Main chat
        Column(modifier = Modifier.weight(1f).fillMaxHeight().background(DiscordPalette.DarkBackground)) {
            chatContent()
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create or Join Group", fontFamily = PingchatFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Groups are like Discord servers - one tap. No complicated steps.", color = DiscordPalette.Grey400, fontSize = 12.sp)
                    OutlinedTextField(value = newGroupName, onValueChange = { newGroupName = it }, label = { Text("Group name e.g. Friends") }, singleLine = true, shape = RoundedCornerShape(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            if (newGroupName.isNotBlank()) { onJoinGroup(newGroupName.trim()); newGroupName=""; showCreateDialog=false }
                        }, modifier = Modifier.weight(1f)) { Text("Join") }
                        Button(onClick = {
                            if (newGroupName.isNotBlank()) { onCreateGroup(newGroupName.trim()); newGroupName=""; showCreateDialog=false }
                        }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = accent)) { Text("Create") }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showCreateDialog=false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun SimpleChannelList(
    channels: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    accent: Color
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item {
            Text("TEXT CHANNELS", color = DiscordPalette.Grey400, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp,4.dp))
        }
        items(channels) { ch ->
            val isSel = ch == selected
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(if (isSel) Color.White.copy(0.08f) else Color.Transparent).clickable { onSelect(ch) }.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.Tag, contentDescription = null, tint = if (isSel) DiscordPalette.Grey100 else DiscordPalette.Grey400, modifier = Modifier.size(18.dp))
                Text("# $ch", color = if (isSel) DiscordPalette.Grey100 else DiscordPalette.Grey400, fontSize = 13.sp, fontWeight = if (isSel) FontWeight.Medium else FontWeight.Normal, fontFamily = PingchatFontFamily)
            }
        }
        item { Spacer(Modifier.height(12.dp)); Text("VOICE CHANNELS", color = DiscordPalette.Grey400, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp,4.dp)) }
        item {
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Color.Transparent).padding(8.dp,6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Group, contentDescription = null, tint = DiscordPalette.Grey400, modifier = Modifier.size(18.dp))
                Text("General", color = DiscordPalette.Grey400, fontSize = 13.sp)
            }
        }
    }
}
