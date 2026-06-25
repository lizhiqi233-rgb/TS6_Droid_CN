package dev.tsdroid.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object AnimeWallpaperState {
    val currentUrl = mutableStateOf<String?>(null)
    private var fetched = false

    suspend fun ensureFetched() {
        if (fetched) return
        fetched = true
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://www.loliapi.com/acg/pe/")
                val conn = url.openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = false
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val redirect = conn.getHeaderField("Location")
                val finalUrl = if (!redirect.isNullOrBlank()) redirect else conn.url.toString()
                conn.disconnect()
                currentUrl.value = finalUrl
            } catch (_: Exception) {
                fetched = false
            }
        }
    }
}

@Composable
fun AnimeBackground(enabled: Boolean) {
    if (!enabled) return

    LaunchedEffect(Unit) {
        AnimeWallpaperState.ensureFetched()
    }

    val url = AnimeWallpaperState.currentUrl.value

    var imageLoaded by remember { mutableStateOf(false) }
    val imageAlpha by animateFloatAsState(
        targetValue = if (imageLoaded) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "bgFadeIn",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.75f)
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(imageAlpha),
                onSuccess = { imageLoaded = true },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.03f),
                            Color.White.copy(alpha = 0.08f),
                        )
                    )
                )
        )
    }
}
