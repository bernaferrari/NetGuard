package com.bernaferrari.quietguard.ui.util

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bernaferrari.quietguard.ui.theme.LocalMotion
import com.bernaferrari.quietguard.ui.theme.spacing
import com.bernaferrari.quietguard.generated.resources.Res
import com.bernaferrari.quietguard.generated.resources.action_retry
import com.bernaferrari.quietguard.generated.resources.ui_load_failed
import com.bernaferrari.quietguard.generated.resources.ui_load_failed_body
import org.jetbrains.compose.resources.stringResource

import com.bernaferrari.quietguard.ui.icons.Icon
import com.bernaferrari.quietguard.ui.icons.MaterialIcon
import com.bernaferrari.quietguard.ui.icons.MaterialSymbols

enum class StatePlaceholderTone {
    Neutral,
    Error,
}

@Composable
fun LoadErrorPlaceholder(
    icon: MaterialIcon,
    onRetry: () -> Unit,
) {
    StatePlaceholder(
        title = stringResource(Res.string.ui_load_failed),
        message = stringResource(Res.string.ui_load_failed_body),
        icon = icon,
        actionLabel = stringResource(Res.string.action_retry),
        actionIcon = MaterialSymbols.Filled.Refresh,
        onAction = onRetry,
        tone = StatePlaceholderTone.Error,
    )
}

/**
 * A reusable placeholder component for empty, loading, and error states.
 * Features subtle pulse animation during loading for a professional feel.
 */
@Composable
fun StatePlaceholder(
    title: String,
    message: String,
    icon: MaterialIcon,
    secondaryMessage: String? = null,
    actionLabel: String? = null,
    actionIcon: MaterialIcon? = null,
    onAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    isLoading: Boolean = false,
    tone: StatePlaceholderTone = StatePlaceholderTone.Neutral,
) {
    val spacing = MaterialTheme.spacing
    val motion = LocalMotion.current

    // Subtle pulse animation for loading state only.
    val pulseAlpha =
        if (isLoading && !motion.reducedMotion) {
            val infiniteTransition = rememberInfiniteTransition(label = "loadingPulse")
            val animatedAlpha by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(motion.durationSlow, easing = motion.easingStandard),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "pulseAlpha",
            )
            animatedAlpha
        } else {
            1f
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.extraLarge, vertical = spacing.xxLarge),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 360.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(34.dp)
                            .alpha(pulseAlpha),
                        strokeWidth = 3.dp,
                    )
                }
            } else {
                val iconContainerColor = when (tone) {
                    StatePlaceholderTone.Neutral -> MaterialTheme.colorScheme.surfaceContainerHigh
                    StatePlaceholderTone.Error -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)
                }
                val iconContentColor = when (tone) {
                    StatePlaceholderTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
                    StatePlaceholderTone.Error -> MaterialTheme.colorScheme.onErrorContainer
                }
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = iconContainerColor,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon = icon,
                            contentDescription = null,
                            tint = iconContentColor,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(spacing.large))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(spacing.small))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (!secondaryMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(spacing.extraSmall))
                Text(
                    text = secondaryMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(spacing.large))
                FilledTonalButton(onClick = onAction) {
                    if (actionIcon != null) {
                        Icon(
                            icon = actionIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.size(spacing.small))
                    }
                    Text(text = actionLabel)
                }
            }
            if (secondaryActionLabel != null && onSecondaryAction != null) {
                Spacer(modifier = Modifier.height(spacing.extraSmall))
                TextButton(onClick = onSecondaryAction) {
                    Text(text = secondaryActionLabel)
                }
            }
        }
    }
}
