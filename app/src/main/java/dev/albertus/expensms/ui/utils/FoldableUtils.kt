package dev.albertus.expensms.ui.utils

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Information about the current foldable state
 */
data class FoldableInfo(
    val isTableTop: Boolean = false,
    val isSeparating: Boolean = false,
    val foldBounds: Rect? = null,
    val foldOrientation: FoldingFeature.Orientation? = null,
    val hingeWidthDp: Float = 0f
)

/**
 * Composable that provides information about foldable device state
 */
@Composable
fun rememberFoldableInfo(): FoldableInfo {
    val context = LocalContext.current
    val density = LocalDensity.current
    var foldableInfo by remember { mutableStateOf(FoldableInfo()) }

    DisposableEffect(context) {
        val activity = context as? Activity
        if (activity != null) {
            val windowInfoTracker = WindowInfoTracker.getOrCreate(activity)
            val job = CoroutineScope(Dispatchers.Main).launch {
                windowInfoTracker.windowLayoutInfo(activity).collect { layoutInfo ->
                    val foldingFeature = layoutInfo.displayFeatures
                        .filterIsInstance<FoldingFeature>()
                        .firstOrNull()

                    if (foldingFeature != null) {
                        val foldBounds = foldingFeature.bounds.toComposeRect()
                        val hingeWidthPx = when (foldingFeature.orientation) {
                            FoldingFeature.Orientation.VERTICAL -> foldingFeature.bounds.width()
                            FoldingFeature.Orientation.HORIZONTAL -> foldingFeature.bounds.height()
                            else -> 0
                        }
                        val hingeWidthDp = with(density) { hingeWidthPx.toDp().value }

                        foldableInfo = FoldableInfo(
                            isTableTop = foldingFeature.state == FoldingFeature.State.HALF_OPENED &&
                                    foldingFeature.orientation == FoldingFeature.Orientation.HORIZONTAL,
                            isSeparating = foldingFeature.isSeparating,
                            foldBounds = foldBounds,
                            foldOrientation = foldingFeature.orientation,
                            hingeWidthDp = hingeWidthDp
                        )

                        Log.d("FoldableUtils", "Fold detected:")
                        Log.d("FoldableUtils", "  State: ${foldingFeature.state}")
                        Log.d("FoldableUtils", "  Orientation: ${foldingFeature.orientation}")
                        Log.d("FoldableUtils", "  Is separating: ${foldingFeature.isSeparating}")
                        Log.d("FoldableUtils", "  Bounds: ${foldingFeature.bounds}")
                        Log.d("FoldableUtils", "  Hinge width: ${hingeWidthDp}dp")
                    } else {
                        foldableInfo = FoldableInfo()
                        Log.d("FoldableUtils", "No fold detected - using regular layout")
                    }
                }
            }

            onDispose {
                job.cancel()
            }
        } else {
            onDispose { }
        }
    }

    return foldableInfo
}

/**
 * Determines if we should use a two-pane layout based on foldable state and screen size
 */
@Composable
fun shouldUseTwoPaneLayout(): Boolean {
    val foldableInfo = rememberFoldableInfo()
    val context = LocalContext.current
    val density = LocalDensity.current
    
    // Get screen width in dp
    val screenWidthDp = with(density) {
        context.resources.displayMetrics.widthPixels.toDp()
    }
    
    return when {
        // If we have a separating fold (like Galaxy Z Fold), use two-pane layout
        foldableInfo.isSeparating && foldableInfo.foldOrientation == FoldingFeature.Orientation.VERTICAL -> {
            Log.d("FoldableUtils", "Using two-pane: separating vertical fold detected")
            true
        }
        // For large screens without fold
        screenWidthDp >= 600.dp -> {
            Log.d("FoldableUtils", "Using two-pane: large screen (${screenWidthDp})")
            true
        }
        else -> {
            Log.d("FoldableUtils", "Using single-pane: screen too small (${screenWidthDp})")
            false
        }
    }
}
