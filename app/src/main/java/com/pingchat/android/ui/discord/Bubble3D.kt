package com.pingchat.android.ui.discord

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pingchat.android.ui.theme.PingchatFontFamily

// 3D bubble - elevated card with gradient & shadow, not just lines
@Composable
fun Bubble3D(
    isOwn: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = if (isOwn) RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp) else RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
    val bg = if (isOwn) {
        Brush.linearGradient(listOf(accent.copy(0.95f), accent.copy(0.75f)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF3A3C42), Color(0xFF2B2D31)))
    }
    Box(
        modifier = modifier
            .shadow(8.dp, shape, spotColor = accent.copy(0.3f), ambientColor = Color.Black.copy(0.4f))
            .clip(shape)
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(content = content)
    }
}

@Composable
fun DiscordMessageBubble(
    sender: String,
    time: String,
    message: String,
    isOwn: Boolean,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
    ) {
        if (!isOwn) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(accent.copy(0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(sender.take(1).uppercase(), color = accent, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = PingchatFontFamily)
            }
            Spacer(Modifier.width(10.dp))
        }
        Column(modifier = Modifier.weight(1f, fill = false), horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(sender, color = if (isOwn) accent else DiscordPalette.Grey100, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, fontFamily = PingchatFontFamily)
                Text(time, color = DiscordPalette.Grey400, fontSize = 11.sp, fontFamily = PingchatFontFamily)
            }
            Spacer(Modifier.height(4.dp))
            Bubble3D(isOwn = isOwn, accent = accent) {
                Text(message, color = Color.White, fontSize = 14.sp, fontFamily = PingchatFontFamily, lineHeight = 18.sp)
            }
        }
        if (isOwn) {
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(accent.copy(0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(sender.take(1).uppercase(), color = accent, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = PingchatFontFamily)
            }
        }
    }
}
