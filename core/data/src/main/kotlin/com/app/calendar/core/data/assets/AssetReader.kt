package com.app.calendar.core.data.assets

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Named

/** Reads bundled JSON assets off the main thread. */
internal class AssetReader @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named(IO_DISPATCHER) private val io: CoroutineDispatcher,
) {
    val json = Json { ignoreUnknownKeys = true }

    suspend fun readTextOrNull(path: String): String? = withContext(io) {
        try {
            context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: FileNotFoundException) {
            null
        }
    }

    companion object {
        const val IO_DISPATCHER = "io"
    }
}
