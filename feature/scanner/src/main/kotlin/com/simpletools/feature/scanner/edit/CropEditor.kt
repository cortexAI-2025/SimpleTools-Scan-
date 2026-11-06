package com.simpletools.feature.scanner.edit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.simpletools.core.scanengine.Corner
import com.simpletools.core.scanengine.CropQuad
import com.simpletools.core.scanengine.NormalizedPoint
import com.simpletools.feature.scanner.R
import kotlin.math.min
import kotlin.math.roundToInt

private val HandleSize = 48.dp

/**
 * The photo with four draggable corner handles. The image is fitted in the available space
 * with a margin of half a handle, so corners on the edge of the photo stay easy to grab.
 */
@Composable
internal fun CropEditor(
    photo: ImageBitmap,
    quad: CropQuad,
    onCornerMoved: (Corner, NormalizedPoint) -> Unit,
    onCornerReleased: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val currentQuad by rememberUpdatedState(quad)
    val currentOnMoved by rememberUpdatedState(onCornerMoved)
    val currentOnReleased by rememberUpdatedState(onCornerReleased)
    val outline = MaterialTheme.colorScheme.primary
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val handlePx = with(density) { HandleSize.toPx() }
        val availableW = constraints.maxWidth - handlePx
        val availableH = constraints.maxHeight - handlePx
        val scale = min(availableW / photo.width, availableH / photo.height).coerceAtLeast(0.01f)
        val imageW = photo.width * scale
        val imageH = photo.height * scale
        val imageWDp = with(density) { imageW.toDp() }
        val imageHDp = with(density) { imageH.toDp() }

        Box(Modifier.size(imageWDp + HandleSize, imageHDp + HandleSize)) {
            val photoDescription = stringResource(R.string.scan_crop_photo)
            Box(
                Modifier
                    .padding(HandleSize / 2)
                    .size(imageWDp, imageHDp)
                    .testTag(CROP_IMAGE_TAG),
            ) {
                Image(
                    bitmap = photo,
                    contentDescription = photoDescription,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize(),
                )
                Canvas(Modifier.fillMaxSize()) {
                    val pts = currentQuad.corners.map { Offset(it.x * size.width, it.y * size.height) }
                    val shape = Path().apply {
                        moveTo(pts[0].x, pts[0].y)
                        pts.drop(1).forEach { lineTo(it.x, it.y) }
                        close()
                    }
                    val outside = Path().apply {
                        fillType = PathFillType.EvenOdd
                        addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
                        addPath(shape)
                    }
                    drawPath(outside, Color.Black.copy(alpha = 0.45f))
                    drawPath(shape, outline, style = Stroke(width = 2.dp.toPx()))
                }
            }
            Corner.entries.forEach { corner ->
                val point = quad[corner]
                val description = stringResource(corner.labelRes())
                Box(
                    Modifier
                        .offset { IntOffset((point.x * imageW).roundToInt(), (point.y * imageH).roundToInt()) }
                        .size(HandleSize)
                        .testTag(cornerTag(corner))
                        .semantics { contentDescription = description }
                        .pointerInput(corner, imageW, imageH) {
                            // No touch slop: the corner follows the finger exactly from the first move.
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                down.consume()
                                // Accumulated here, not read back from the state, so no move is lost
                                // between two frames; the quad clamps it inside the photo.
                                val start = currentQuad[corner]
                                var x = start.x
                                var y = start.y
                                while (true) {
                                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                    val drag = change.positionChange()
                                    if (drag != Offset.Zero) {
                                        change.consume()
                                        x += drag.x / imageW
                                        y += drag.y / imageH
                                        currentOnMoved(corner, NormalizedPoint(x, y))
                                    }
                                    if (!change.pressed) break
                                }
                                currentOnReleased()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(22.dp)
                            .background(outline.copy(alpha = 0.35f), CircleShape)
                            .border(3.dp, outline, CircleShape),
                    )
                }
            }
        }
    }
}

private fun Corner.labelRes(): Int = when (this) {
    Corner.TopLeft -> R.string.scan_corner_top_left
    Corner.TopRight -> R.string.scan_corner_top_right
    Corner.BottomRight -> R.string.scan_corner_bottom_right
    Corner.BottomLeft -> R.string.scan_corner_bottom_left
}

const val CROP_IMAGE_TAG = "scan_crop_image"
const val CORRECTED_PREVIEW_TAG = "scan_corrected_preview"

fun cornerTag(corner: Corner) = "scan_corner_${corner.name}"
