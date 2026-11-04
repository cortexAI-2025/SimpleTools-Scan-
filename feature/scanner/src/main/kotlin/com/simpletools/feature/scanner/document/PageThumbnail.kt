package com.simpletools.feature.scanner.document

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.MaterialTheme
import com.simpletools.feature.scanner.image.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** A small, downsampled image of a page, decoded off the main thread. */
@Composable
internal fun PageThumbnail(
    file: File,
    processor: ImageProcessor,
    contentDescription: String,
    modifier: Modifier = Modifier,
    maxSide: Int = 480,
) {
    val bitmap by produceState<ImageBitmap?>(null, file.path) {
        value = withContext(Dispatchers.IO) { processor.decodeSampled(file, maxSide)?.asImageBitmap() }
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        bitmap?.let {
            Image(bitmap = it, contentDescription = contentDescription, contentScale = ContentScale.Fit, modifier = Modifier.matchParentSize())
        }
    }
}
