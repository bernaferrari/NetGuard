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
internal val filledInbox: ImageVector
  get() {
    if (_filledInbox != null) {
      return _filledInbox!!
    }
    _filledInbox =
      ImageVector.Builder(
          name = "inbox",
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
            moveTo(5f, 21f)
            quadTo(4.18f, 21f, 3.59f, 20.41f)
            reflectiveQuadTo(3f, 19f)
            verticalLineTo(5f)
            quadTo(3f, 4.17f, 3.59f, 3.59f)
            reflectiveQuadTo(5f, 3f)
            horizontalLineTo(19f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(21f, 5f)
            verticalLineTo(19f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(19f, 21f)
            horizontalLineTo(5f)
            close()
            moveToRelative(7f, -5f)
            quadToRelative(0.8f, 0f, 1.48f, -0.41f)
            reflectiveQuadToRelative(1.1f, -1.09f)
            quadToRelative(0.15f, -0.23f, 0.38f, -0.36f)
            reflectiveQuadTo(15.45f, 14f)
            horizontalLineTo(19f)
            verticalLineTo(5f)
            horizontalLineTo(5f)
            verticalLineToRelative(9f)
            horizontalLineTo(8.55f)
            quadToRelative(0.28f, 0f, 0.5f, 0.14f)
            reflectiveQuadTo(9.43f, 14.5f)
            quadToRelative(0.43f, 0.67f, 1.1f, 1.09f)
            reflectiveQuadTo(12f, 16f)
            close()
          }
        }
        .build()
    return _filledInbox!!
  }

internal var _filledInbox: ImageVector? = null
