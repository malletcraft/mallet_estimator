package com.malletcrafts.sitephotos

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malletcrafts.sitephotos.pano.Viewport

/** What a double-tap jumps to. The complaint was that reading the numbers
 *  took work, so the gesture that costs least has to land somewhere already
 *  legible rather than one notch on the way there. */
private const val DOUBLE_TAP_SCALE = 3f

/**
 * Decode target for the full-screen viewer, on the long edge.
 *
 * The viewer used to share the grid's decode at 1600 px, which is fine for
 * recognising a wall and useless for reading a number written on it: zooming a
 * 1600 px decode past about 2x magnifies the sampler, not the photograph. 3072
 * covers a face at its native size — the splitter's faces are ~2400 px — while
 * still halving anything bigger instead of trying to hold it whole.
 */
const val VIEWER_TARGET = 3072

/**
 * One photograph, pinch to zoom and drag to move around.
 *
 * Amit, 2026-08-23: "need pinch zoom on apk annotated fotos. hard to read it
 * currently." An annotated face is an ImageMeter export whose entire payload
 * is small text written over the picture, and it was being shown scaled to fit
 * a phone with no way to get closer.
 *
 * Deliberately NOT the annotation editor's viewport code. That one carries
 * measurement geometry — it converts between screen and the image's normalized
 * space on every touch, because what it draws has to land on the pixel a
 * person pointed at. Here nothing is drawn over the photo, so the transform is
 * one graphicsLayer and the arithmetic that would have to stay in step with
 * the editor's simply does not exist. The part that IS shared is the part
 * worth sharing: [Viewport], which is unit-tested in the pano module.
 */
@Composable
fun ZoomableImage(
    source: ThumbSource?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    /** Changing this snaps back to fit. Pass the identity of the PHOTOGRAPH,
     *  not of the bytes on screen: toggling Original/Annotated is the same
     *  wall from the same place, and resetting there would throw the zoom away
     *  at the exact moment a person is flicking between the two to compare. */
    resetKey: Any? = null,
) {
    val context = LocalContext.current
    val key = when (source) {
        is ThumbSource.LocalFile -> "f:${source.path}:$VIEWER_TARGET"
        is ThumbSource.Content -> "u:${source.uri}:$VIEWER_TARGET"
        null -> null
    }
    var bmp by remember(key) { mutableStateOf<Bitmap?>(key?.let { Thumbs.cached(it) }) }
    LaunchedEffect(key) {
        if (bmp != null || source == null) return@LaunchedEffect
        bmp = when (source) {
            is ThumbSource.LocalFile -> Thumbs.file(source.path, VIEWER_TARGET)
            is ThumbSource.Content -> Thumbs.uri(context, source.uri, VIEWER_TARGET)
        }
    }

    var box by remember { mutableStateOf(IntSize.Zero) }
    var view by remember(resetKey) { mutableStateOf(Viewport.FIT) }

    val boxW = box.width.toFloat()
    val boxH = box.height.toFloat()
    val fit = Viewport.fitScale(
        (bmp?.width ?: 0).toFloat(), (bmp?.height ?: 0).toFloat(), boxW, boxH)
    val fitW = (bmp?.width ?: 0).toFloat() * fit
    val fitH = (bmp?.height ?: 0).toFloat() * fit
    val ready = fit > 0f

    Box(
        modifier
            .background(Color(0xFF0B0D0D))
            .onSizeChanged { box = it }
            .pointerInput(bmp, box) {
                detectTapGestures(onDoubleTap = { p ->
                    if (!ready) return@detectTapGestures
                    view =
                        // Toward the tapped point on the way in; dead centre on
                        // the way out, because "put it back" has one answer.
                        if (view.zoomed) Viewport.FIT
                        else view.zoomAbout(p.x, p.y, DOUBLE_TAP_SCALE / view.scale,
                            fitW, fitH, boxW, boxH)
                })
            }
            .pointerInput(bmp, box) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    if (!ready) return@detectTransformGestures
                    // One finger moves the picture, two also resize it. The
                    // zoom is applied first so the pan is measured in the
                    // scale the fingers just finished asking for.
                    view = view
                        .zoomAbout(centroid.x, centroid.y, zoom, fitW, fitH, boxW, boxH)
                        .pan(pan.x, pan.y, fitW, fitH, boxW, boxH)
                }
            },
    ) {
        bmp?.let {
            Image(
                it.asImageBitmap(), contentDescription,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = view.scale; scaleY = view.scale
                        translationX = view.offX; translationY = view.offY
                    },
                contentScale = ContentScale.Fit)
        }
        // Say what the screen can do, then get out of the way. At fit the
        // affordance is invisible and has to be stated; once zoomed the only
        // questions left are how far in you are and how to get back.
        Text(
            if (view.zoomed) "%.1f×  ·  double-tap to fit".format(view.scale)
            else "Pinch to zoom  ·  double-tap to magnify",
            color = Color(0xFFEDEFEA), fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color.Black.copy(alpha = .42f))
                .padding(horizontal = 9.dp, vertical = 4.dp))
    }
}
