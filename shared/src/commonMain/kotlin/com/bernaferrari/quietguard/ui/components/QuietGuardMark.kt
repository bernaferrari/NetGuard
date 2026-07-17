package com.bernaferrari.quietguard.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** The shield-and-crescent mark shared with the QuietGuard launcher icon. */
val QuietGuardMark: ImageVector by lazy {
    ImageVector.Builder(
        name = "QuietGuardMark",
        defaultWidth = 108.dp,
        defaultHeight = 108.dp,
        viewportWidth = 108f,
        viewportHeight = 108f,
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(54f, 22f)
            lineTo(78f, 31f)
            lineTo(78f, 51f)
            curveTo(78f, 68f, 68f, 81f, 54f, 87f)
            curveTo(40f, 81f, 30f, 68f, 30f, 51f)
            lineTo(30f, 31f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(51f, 42f)
            curveTo(43f, 44f, 39f, 51f, 41f, 59f)
            curveTo(43f, 67f, 51f, 71f, 59f, 68f)
            curveTo(53f, 67f, 49f, 63f, 48f, 57f)
            curveTo(47f, 51f, 49f, 46f, 54f, 43f)
            curveTo(53f, 42f, 52f, 42f, 51f, 42f)
            close()
        }
    }.build()
}
