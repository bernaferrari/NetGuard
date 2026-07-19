package com.bernaferrari.quietguard.ui.icons

// Generated from Google Material Symbols Rounded's Kotlin vector endpoint.
// The FILL axis is explicit: FILL=1 for Filled and FILL=0 for Outlined.
// opsz=24, wght=400, GRAD=0, ROND=50.
// Source: https://fonts.gstatic.com/render/v1/Material+Symbols+Rounded/24dp/<name>.kt

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
internal val filledEqualizer: ImageVector
  get() {
    if (_filledEqualizer != null) {
      return _filledEqualizer!!
    }
    _filledEqualizer =
      ImageVector.Builder(
          name = "equalizer",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(4.59f, 19.41f)
            quadTo(4f, 18.83f, 4f, 18f)
            verticalLineTo(14f)
            quadTo(4f, 13.18f, 4.59f, 12.59f)
            reflectiveQuadTo(6f, 12f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            quadTo(8f, 13.18f, 8f, 14f)
            verticalLineToRelative(4f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(6f, 20f)
            reflectiveQuadTo(4.59f, 19.41f)
            close()
            moveTo(12f, 20f)
            quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
            reflectiveQuadTo(10f, 18f)
            verticalLineTo(6f)
            quadTo(10f, 5.18f, 10.59f, 4.59f)
            reflectiveQuadTo(12f, 4f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            quadTo(14f, 5.18f, 14f, 6f)
            verticalLineTo(18f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(12f, 20f)
            close()
            moveToRelative(4.59f, -0.59f)
            quadTo(16f, 18.83f, 16f, 18f)
            verticalLineTo(11f)
            quadToRelative(0f, -0.83f, 0.59f, -1.41f)
            reflectiveQuadTo(18f, 9f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            reflectiveQuadTo(20f, 11f)
            verticalLineToRelative(7f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(18f, 20f)
            reflectiveQuadTo(16.59f, 19.41f)
            close()
          }
        }
        .build()
    return _filledEqualizer!!
  }

internal var _filledEqualizer: ImageVector? = null
