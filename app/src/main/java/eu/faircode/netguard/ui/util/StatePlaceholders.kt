package eu.faircode.netguard.ui.util

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import eu.faircode.netguard.ui.theme.LocalMotion
import eu.faircode.netguard.ui.theme.spacing

/**
 * A reusable placeholder component for empty, loading, and error states.
 * Features subtle pulse animation during loading for a professional feel.
 */
@Composable
fun StatePlaceholder(
    title: String,
    message: String,
    icon: ImageVector,
    secondaryMessage: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    isLoading: Boolean = false,
) {
    val spacing = MaterialTheme.spacing
    val motion = LocalMotion.current

    // Subtle pulse animation for loading state
    val infiniteTransition = rememberInfiniteTransition(label = "loadingPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(motion.durationSlow, easing = motion.easingStandard),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.extraLarge),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.alpha(pulseAlpha),
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = title, // Accessible: describe the state
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(spacing.default))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
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
            Spacer(modifier = Modifier.height(spacing.default))
            FilledTonalButton(onClick = onAction) {
                Text(text = actionLabel)
            }
        }
        if (secondaryActionLabel != null && onSecondaryAction != null) {
            Spacer(modifier = Modifier.height(spacing.small))
            TextButton(onClick = onSecondaryAction) {
                Text(text = secondaryActionLabel)
            }
        }
    }
}
