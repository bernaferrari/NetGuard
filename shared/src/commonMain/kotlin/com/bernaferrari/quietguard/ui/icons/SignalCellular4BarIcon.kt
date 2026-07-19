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
internal val filledSignalCellular4Bar: ImageVector
  get() {
    if (_filledSignalCellular4Bar != null) {
      return _filledSignalCellular4Bar!!
    }
    _filledSignalCellular4Bar =
      ImageVector.Builder(
          name = "signal_cellular_4_bar",
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
            moveTo(4.43f, 22f)
            quadTo(3.98f, 22f, 3.69f, 21.7f)
            reflectiveQuadTo(3.4f, 21f)
            quadToRelative(0f, -0.2f, 0.07f, -0.38f)
            reflectiveQuadTo(3.7f, 20.3f)
            lineTo(20.3f, 3.7f)
            quadTo(20.45f, 3.55f, 20.63f, 3.47f)
            reflectiveQuadTo(21f, 3.4f)
            quadToRelative(0.4f, 0f, 0.7f, 0.29f)
            quadTo(22f, 3.97f, 22f, 4.42f)
            verticalLineTo(20.5f)
            quadToRelative(0f, 0.63f, -0.44f, 1.06f)
            reflectiveQuadTo(20.5f, 22f)
            horizontalLineTo(4.43f)
            close()
          }
        }
        .build()
    return _filledSignalCellular4Bar!!
  }

internal var _filledSignalCellular4Bar: ImageVector? = null
