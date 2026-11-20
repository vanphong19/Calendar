package com.app.calendar.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.app.calendar.core.model.DailyImage

/**
 * Shows a daily image with a three-step fallback: remote (when allowed) → bundled asset →
 * the image's dominant colour. Never blocks or flashes white while loading.
 */
@Composable
fun DailyImageView(
    image: DailyImage?,
    allowNetwork: Boolean,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val placeholderColor = image?.let { Color(it.dominantColor) } ?: Color(0xFF8C7B6B)
    val remote = image?.remoteUrl?.takeIf { allowNetwork }
    val local = image?.localAsset?.let { "file:///android_asset/$it" }
    var useLocal by remember(image, allowNetwork) { mutableStateOf(remote == null) }
    val model = if (useLocal) local else remote

    if (model == null) {
        Box(modifier.background(placeholderColor))
        return
    }
    val context = LocalContext.current
    val painter = remember(placeholderColor) { ColorPainter(placeholderColor) }
    AsyncImage(
        model = remember(model) { ImageRequest.Builder(context).data(model).crossfade(true).build() },
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        placeholder = painter,
        error = painter,
        onError = { if (!useLocal && local != null) useLocal = true },
    )
}
