package com.app.calendar.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
 * the image's dominant colour (plus optional decorative [fallbackContent] while loading or offline).
 */
@Composable
fun DailyImageView(
    image: DailyImage?,
    allowNetwork: Boolean,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackContent: (@Composable BoxScope.() -> Unit)? = null,
) {
    val placeholderColor = image?.let { Color(it.dominantColor) } ?: Color(0xFF8C7B6B)
    val remote = image?.remoteUrl?.takeIf { allowNetwork }
    val local = image?.localAsset?.let { "file:///android_asset/$it" }
    var useLocal by remember(image, allowNetwork) { mutableStateOf(remote == null) }
    var loaded by remember(image, allowNetwork, useLocal) { mutableStateOf(false) }
    val model = if (useLocal) local else remote

    Box(modifier) {
        if (model == null) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(
                        listOf(
                            placeholderColor,
                            placeholderColor.copy(alpha = 0.82f),
                        ),
                    ),
                ),
            )
            fallbackContent?.invoke(this)
        } else {
            val context = LocalContext.current
            val painter = remember(placeholderColor) { ColorPainter(placeholderColor) }
            AsyncImage(
                model = remember(model) { ImageRequest.Builder(context).data(model).crossfade(true).build() },
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                placeholder = painter,
                error = painter,
                onSuccess = { loaded = true },
                onError = {
                    loaded = false
                    if (!useLocal && local != null) useLocal = true
                },
            )
            if (!loaded && fallbackContent != null) {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f)),
                        ),
                    ),
                )
                fallbackContent()
            }
        }
    }
}
