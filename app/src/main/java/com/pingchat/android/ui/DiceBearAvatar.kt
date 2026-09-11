package com.pingchat.android.ui

import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import com.pingchat.android.ui.theme.PingchatFontFamily
import java.net.URLEncoder

/**
 * DiceBear Identicon avatar provider for procedural identity.
 * Uses Noise session publicKey as seed to generate deterministic unique avatars.
 * Aggressive disk + memory caching ensures instant offline loads.
 *
 * Endpoint: https://api.dicebear.com/7.x/identicon/svg?seed={publicKey}
 */
object DiceBearConfig {
    private const val BASE_URL = "https://api.dicebear.com/7.x/identicon/svg"
    private const val CACHE_MAX_SIZE_PERCENT = 0.02

    fun urlForSeed(seed: String): String {
        val encoded = try {
            URLEncoder.encode(seed, "UTF-8")
        } catch (_: Exception) {
            seed
        }
        // identicon is deterministic; add background and size params for consistency
        return "$BASE_URL?seed=$encoded&backgroundColor=transparent&radius=50"
    }

    fun imageLoader(context: Context): ImageLoader {
        // Reuse singleton if already built via coil; build custom with SVG support and aggressive cache
        return ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(CACHE_MAX_SIZE_PERCENT)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("dicebear_avatars"))
                    .maxSizePercent(CACHE_MAX_SIZE_PERCENT)
                    .build()
            }
            .respectCacheHeaders(false) // aggressive: serve stale while offline
            .crossfade(true)
            .build()
    }
}

/**
 * Procedural avatar that loads DiceBear SVG from publicKey seed.
 * Falls back to initials on error or when offline without cache.
 *
 * Edge cases handled:
 * - null/blank publicKey -> initials immediately (no network)
 * - offline with cache -> serve from diskCache instantly (CachePolicy.ENABLED)
 * - offline without cache -> error fallback to initials (no blank avatar)
 * - malformed seed -> still deterministic via identicon, fallback handles decode failure
 * - SVG decode failure -> fallback to initials (never shows broken image)
 */
@Composable
fun DiceBearAvatar(
    publicKey: String?,
    name: String,
    color: Color,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    theyFavoritedUs: Boolean = false,
    isVerified: Boolean = false,
    badge: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    if (publicKey.isNullOrBlank()) {
        PeerAvatar(
            name = name,
            color = color,
            modifier = modifier,
            isFavorite = isFavorite,
            theyFavoritedUs = theyFavoritedUs,
            isVerified = isVerified,
            badge = badge
        )
        return
    }

    val imageLoader = remember(context) { DiceBearConfig.imageLoader(context) }
    val url = remember(publicKey) { DiceBearConfig.urlForSeed(publicKey) }
    val palette = com.pingchat.android.ui.theme.LocalPingchatPalette.current
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier.size(42.dp),
        contentAlignment = Alignment.Center
    ) {
        // 38dp inner circle - image sits on top of initials fallback
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            // Initials behind image - visible if DiceBear fails (offline no cache, svg error)
            Text(
                text = name.trim().firstOrNull()?.uppercase() ?: "#",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = PingchatFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = color
            )
            // DiceBear identicon on top - aggressive cache so offline loads instantly
            coil.compose.SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(url)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCacheKey(url)
                    .memoryCacheKey(url)
                    .crossfade(true)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = "Avatar for $name",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape),
                loading = {
                    // While loading first time, keep initials visible - no spinner needed
                },
                error = {
                    // Keep initials - already drawn behind, just keep transparent
                }
            )
        }

        if (badge != null) {
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .size(PeerAvatarBadgeSize)
                    .align(Alignment.BottomEnd),
                shape = CircleShape,
                color = colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Box(contentAlignment = Alignment.Center) { badge() }
            }
        }

        if (isFavorite || theyFavoritedUs) {
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopEnd),
                shape = CircleShape,
                color = colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Icon(
                        painter = androidx.compose.ui.res.painterResource(
                            if (isFavorite) com.pingchat.android.R.drawable.ic_spec_star_filled else com.pingchat.android.R.drawable.ic_spec_star
                        ),
                        contentDescription = androidx.compose.ui.res.stringResource(
                            if (isFavorite) com.pingchat.android.R.string.cd_favorite else com.pingchat.android.R.string.cd_favorited_you
                        ),
                        modifier = Modifier.size(10.dp),
                        tint = palette.accentOrange
                    )
                }
            }
        }

        if (isVerified) {
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopStart),
                shape = CircleShape,
                color = colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = androidx.compose.ui.res.stringResource(com.pingchat.android.R.string.fingerprint_verified_label),
                        modifier = Modifier.size(12.dp),
                        tint = colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Enhanced PeerAvatar that automatically uses DiceBear when publicKey is available.
 * This is the migration path: existing calls without publicKey continue to show initials,
 * new calls with Noise publicKey show procedural identicons.
 */
@Composable
fun PeerAvatarWithDiceBear(
    name: String,
    color: Color,
    publicKey: String? = null,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    theyFavoritedUs: Boolean = false,
    isVerified: Boolean = false,
    badge: (@Composable () -> Unit)? = null
) {
    if (publicKey.isNullOrBlank()) {
        PeerAvatar(
            name = name,
            color = color,
            modifier = modifier,
            isFavorite = isFavorite,
            theyFavoritedUs = theyFavoritedUs,
            isVerified = isVerified,
            badge = badge
        )
    } else {
        DiceBearAvatar(
            publicKey = publicKey,
            name = name,
            color = color,
            modifier = modifier,
            isFavorite = isFavorite,
            theyFavoritedUs = theyFavoritedUs,
            isVerified = isVerified,
            badge = badge
        )
    }
}

/**
 * Utility to derive DiceBear seed from Noise session hex.
 * Normalizes hex to lowercase, trims whitespace, ensures non-empty.
 */
fun noisePublicKeyToDiceBearSeed(hex: ByteArray?): String? {
    if (hex == null || hex.isEmpty()) return null
    return try {
        hex.joinToString("") { "%02x".format(it) }
    } catch (_: Exception) { null }
}

fun noisePublicKeyToDiceBearSeed(hexString: String?): String? {
    if (hexString.isNullOrBlank()) return null
    return hexString.trim().lowercase().takeIf { it.isNotEmpty() }
}
