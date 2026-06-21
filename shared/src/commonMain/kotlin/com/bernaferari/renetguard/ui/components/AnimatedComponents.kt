package com.bernaferari.renetguard.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.bernaferari.renetguard.ui.theme.LocalMotion

/**
 * Animated expand/collapse container for collapsible sections.
 * Uses subtle fade + vertical expand animation.
 */
@Composable
fun ExpandableContent(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    val motion = LocalMotion.current
    AnimatedVisibility(
        visible = expanded,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(motion.durationMedium, easing = motion.easingStandard)
        ) + expandVertically(
            animationSpec = tween(motion.durationMedium, easing = motion.easingEmphasized),
            expandFrom = Alignment.Top
        ),
        exit = fadeOut(
            animationSpec = tween(motion.durationFast, easing = motion.easingAccelerate)
        ) + shrinkVertically(
            animationSpec = tween(motion.durationMedium, easing = motion.easingStandard),
            shrinkTowards = Alignment.Top
        ),
        content = content,
    )
}

/**
 * Subtle fade-in animation for content appearing on screen.
 */
@Composable
fun FadeInContent(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    val motion = LocalMotion.current
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(motion.durationMedium, easing = motion.easingDecelerate)),
        exit = fadeOut(tween(motion.durationFast, easing = motion.easingAccelerate)),
        content = content,
    )
}

/**
 * Clickable container with subtle press scale effect.
 * Professional and non-jarring - scales down to 0.98f on press.
 */
@Composable
fun PressableContent(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "pressScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                enabled = enabled,
                onClick = onClick,
            )
    ) {
        content()
    }
}

/**
 * Modifier extension for smooth content size changes.
 * Uses spring animation for natural feel.
 */
fun Modifier.animateContentHeight(): Modifier = this.animateContentSize(
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
)

/**
 * Animated chevron/expand icon rotation.
 * Rotates 180 degrees when expanded.
 */
@Composable
fun animatedRotation(expanded: Boolean): Float {
    val motion = LocalMotion.current
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(motion.durationMedium, easing = motion.easingStandard),
        label = "iconRotation"
    )
    return rotation
}
