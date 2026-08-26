package com.bitchat.android.smartreply

import android.util.Log
import com.bitchat.android.model.BitchatMessage
import com.google.mlkit.nl.smartreply.SmartReply
import com.google.mlkit.nl.smartreply.SmartReplySuggestion
import com.google.mlkit.nl.smartreply.TextMessage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * On-device Smart Reply manager using Google ML Kit.
 * All processing remains entirely on-device to maintain surveillance resistance of the mesh.
 * No network calls - model runs locally via Play Services ML Kit.
 *
 * Hooks into local message repository (ConversationDatabase / ChatViewModel state).
 * Passes last 3-4 messages of conversation thread into Smart Reply generator.
 *
 * Edge grill:
 * - Offline: works fully offline after model download (on-device)
 * - Empty thread: returns empty suggestions (no crash)
 * - Encrypted/empty content: filters out non-text messages
 * - Group chat vs DM: treats any sender != localUser as remote
 * - Last message isLocalUser=true -> SmartReply returns empty (no suggestions for own message) - we handle by not showing chips
 */
object SmartReplyManager {
    private const val TAG = "SmartReplyManager"
    private const val MAX_INPUT_MESSAGES = 4
    private const val LOCAL_USER_ID = "local_user"

    private val client by lazy { SmartReply.getClient() }

    /**
     * Generate reply suggestions for given conversation snapshot.
     * Takes last [MAX_INPUT_MESSAGES] messages, maps to ML Kit TextMessage, requests suggestions.
     * Returns flow of List<String> suggestions (text only) - empty if none or error.
     */
    fun suggestReplies(
        messages: List<BitchatMessage>,
        localNickname: String
    ): Flow<List<String>> = callbackFlow {
        if (messages.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        // Filter to last 3-4 text messages only - ignore Audio/Image/File and empty content
        val recent = messages
            .filter { it.type == com.bitchat.android.model.BitchatMessageType.Message && it.content.isNotBlank() }
            .takeLast(MAX_INPUT_MESSAGES)

        if (recent.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        // If last message is from local user, Smart Reply will correctly return no suggestions
        // We still query but expect empty; alternatively we can short-circuit to avoid unnecessary model call
        val lastIsLocal = recent.last().sender == localNickname
        if (lastIsLocal && recent.size == 1) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val mlKitMessages = recent.map { msg ->
            val isLocal = msg.sender == localNickname
            // ML Kit requires userId non-empty; use sender name or peerID, fallback to "remote"
            val userId = when {
                isLocal -> LOCAL_USER_ID
                !msg.senderPeerID.isNullOrBlank() -> msg.senderPeerID!!
                msg.sender.isNotBlank() -> msg.sender
                else -> "remote_user"
            }
            TextMessage.createForRemoteUser(
                msg.content,
                msg.timestamp.time,
                userId
            ).let {
                // Mark local correctly: createForRemoteUser then isLocalUser flag via createForLocalUser if needed
                if (isLocal) {
                    TextMessage.createForLocalUser(msg.content, msg.timestamp.time)
                } else {
                    TextMessage.createForRemoteUser(msg.content, msg.timestamp.time, userId)
                }
            }
        }

        // Need local user case handled: ML Kit expects last message not from local? But spec says pass conversation including local.
        // We'll just pass as is; model will handle.

        client.suggestReplies(mlKitMessages)
            .addOnSuccessListener { result ->
                val suggestions = result.suggestions.map { it.text }.filter { it.isNotBlank() }.take(3)
                trySend(suggestions)
                close()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "SmartReply failed: ${e.message}")
                trySend(emptyList())
                close()
            }

        awaitClose { /* no cleanup */ }
    }

    /**
     * Synchronous helper for ViewModel - uses last 3-4 messages directly.
     * Returns suggestions list via callback to avoid blocking.
     */
    fun fetchSuggestions(
        messages: List<BitchatMessage>,
        localNickname: String,
        onResult: (List<String>) -> Unit
    ) {
        val recent = messages
            .filter { it.type == com.bitchat.android.model.BitchatMessageType.Message && it.content.isNotBlank() }
            .takeLast(MAX_INPUT_MESSAGES)

        if (recent.isEmpty()) {
            onResult(emptyList())
            return
        }
        val mlKitMessages = recent.map { msg ->
            val isLocal = msg.sender == localNickname
            if (isLocal) {
                TextMessage.createForLocalUser(msg.content, msg.timestamp.time)
            } else {
                val userId = msg.senderPeerID ?: msg.sender.takeIf { it.isNotBlank() } ?: "remote"
                TextMessage.createForRemoteUser(msg.content, msg.timestamp.time, userId)
            }
        }

        client.suggestReplies(mlKitMessages)
            .addOnSuccessListener { result ->
                onResult(result.suggestions.map { it.text }.filter { it.isNotBlank() }.take(3))
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "SmartReply fetch failed: ${e.message}")
                onResult(emptyList())
            }
    }

    fun close() {
        try { client.close() } catch (_: Exception) {}
    }
}
