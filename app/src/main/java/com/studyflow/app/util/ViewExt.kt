package com.studyflow.app.util

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Apps targeting Android 15+ (API 35/36) draw edge-to-edge, behind the status and navigation bars.
 * This pads the root view by the system-bar/cutout insets (on top of whatever padding the layout
 * already has) so content is never hidden underneath them.
 */
fun View.applySystemBarInsets() {
    val start = paddingLeft
    val top = paddingTop
    val end = paddingRight
    val bottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime()
        )
        view.setPadding(start + bars.left, top + bars.top, end + bars.right, bottom + bars.bottom)
        insets
    }
    ViewCompat.requestApplyInsets(this)
}
