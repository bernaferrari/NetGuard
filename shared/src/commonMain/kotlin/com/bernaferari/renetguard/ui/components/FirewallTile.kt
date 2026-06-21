package com.bernaferari.renetguard.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A beautiful, compact tappable tile for firewall allow/block rules.
 * Fits perfectly in lists or horizontal pairings (72dp standard height).
 */
@Composable
fun FirewallTile(
    allowedIcon: ImageVector,
    blockedIcon: ImageVector,
    label: String,
    allowed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
) {
    val blocked = !allowed
    val density = LocalDensity.current

    // Animate corners: when blocked, increase radius to 28.dp for emphasis.
    // If not blocked, use the precise (and potentially asymmetric) baseShape corners.
    val baseRounded = shape as? RoundedCornerShape ?: RoundedCornerShape(20.dp)

    val currentTopStart = animateCorner(baseRounded.topStart, 28.dp, blocked, density)
    val currentTopEnd = animateCorner(baseRounded.topEnd, 28.dp, blocked, density)
    val currentBottomEnd = animateCorner(baseRounded.bottomEnd, 28.dp, blocked, density)
    val currentBottomStart = animateCorner(baseRounded.bottomStart, 28.dp, blocked, density)

    val animatedShape = RoundedCornerShape(
        topStart = currentTopStart,
        topEnd = currentTopEnd,
        bottomEnd = currentBottomEnd,
        bottomStart = currentBottomStart,
    )

    // Keep tile background behavior; only the icon pill changes between states.
    val containerColor by animateColorAsState(
        targetValue = if (blocked) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "ftBg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (blocked) MaterialTheme.colorScheme.onErrorContainer
        else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(400),
        label = "ftTitle",
    )
    val secondaryColor by animateColorAsState(
        targetValue = if (blocked) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(400),
        label = "ftDetail",
    )
    val iconContainerColor by animateColorAsState(
        targetValue = if (blocked) MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(400),
        label = "ftIconBg",
    )
    val iconColor = if (blocked) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val iconAllowedTint = if (blocked) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.44f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val slashColor = if (blocked) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.76f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }

    Surface(
        onClick = onToggle,
        modifier = modifier.height(72.dp),
        shape = animatedShape,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Icon Pill
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center,
            ) {
                DiagonalWipeIcon(
                    blocked = blocked,
                    allowedIcon = allowedIcon,
                    blockedIcon = blockedIcon,
                    allowedTint = iconAllowedTint,
                    blockedTint = iconColor,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }

            // Text Data
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AnimatedContent(
                    targetState = blocked,
                    transitionSpec = {
                        (slideInVertically { height -> if (targetState) height else -height } + fadeIn()) togetherWith
                                (slideOutVertically { height -> if (targetState) -height else height } + fadeOut())
                    },
                    label = "statusTextAnim",
                ) { isBlocked ->
                    Text(
                        text = if (isBlocked) "Blocked" else "Allowed",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = secondaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun animateCorner(
    base: CornerSize,
    targetDp: Dp,
    blocked: Boolean,
    density: Density
): CornerSize {
    // We convert the CornerSize to Dp based on a nominal square size
    // to allow smooth transition without layout-pass jank if possible.
    val basePx = base.toPx(Size(100f, 100f), density)
    val baseDp = with(density) { basePx.toDp() }

    val currentDp by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (blocked) targetDp else baseDp,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "cornerAnim"
    )
    return CornerSize(currentDp)
}
