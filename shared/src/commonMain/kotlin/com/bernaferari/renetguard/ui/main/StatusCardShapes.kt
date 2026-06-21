package com.bernaferari.renetguard.ui.main

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun rememberStatusBadgeShape(progress: Float): Shape {
    val morph = remember { Morph(start = MaterialShapes.Square, end = MaterialShapes.Cookie9Sided) }
    return rememberMorphShape(morph = morph, progress = progress)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun rememberStatusCardShape(progress: Float): Shape {
    val morph = remember { Morph(start = MaterialShapes.Square, end = MaterialShapes.Gem) }
    return rememberMorphShape(morph = morph, progress = progress)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun rememberMorphShape(
    morph: Morph,
    progress: Float,
): Shape =
    remember(morph) {
        MorphShape(morph = morph, progressProvider = { progress })
    }

/**
 * Stable [Shape] instance that reads morph progress on each outline pass.
 * Recreating the shape object every animation frame can prevent wasm/Skiko from
 * applying [Outline.Generic] clipping on clickable surfaces.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private class MorphShape(
    private val morph: Morph,
    private val progressProvider: () -> Float,
) : Shape {
    private var workPath: Path? = null
    private var lastSize = Size.Unspecified

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val morphPath = morph.toPath(progress = progressProvider().coerceIn(0f, 1f), startAngle = 0)
        if (size != lastSize || workPath == null) {
            lastSize = size
            workPath = Path()
        } else {
            workPath!!.rewind()
        }
        val path = workPath!!
        path.addPath(morphPath)
        path.transform(Matrix().apply { scale(x = size.width, y = size.height) })
        path.translate(size.center - path.getBounds().center)
        return Outline.Generic(path)
    }
}