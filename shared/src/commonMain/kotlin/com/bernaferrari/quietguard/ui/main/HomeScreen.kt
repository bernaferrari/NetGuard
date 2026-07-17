package com.bernaferrari.quietguard.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.quietguard.ui.theme.LocalMotion
import com.bernaferrari.quietguard.ui.theme.spacing
import com.bernaferrari.quietguard.ui.components.QuietGuardMark
import com.bernaferrari.quietguard.generated.resources.Res
import com.bernaferrari.quietguard.generated.resources.app_name
import com.bernaferrari.quietguard.generated.resources.menu_firewall
import com.bernaferrari.quietguard.generated.resources.status_disabled
import com.bernaferrari.quietguard.generated.resources.status_enabled
import com.bernaferrari.quietguard.generated.resources.ui_home_hint
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onToggleEnabled: (Boolean) -> Unit,
) {
    val enabled by viewModel.enabled.collectAsStateWithLifecycle()
    val motion = LocalMotion.current
    val backgroundProgress by animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        animationSpec = tween(
            durationMillis = motion.durationSlow * 2,
            easing = FastOutSlowInEasing,
        ),
        label = "homeShaderColorProgress",
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            FirewallStateShaderBackground(
                enabledProgress = backgroundProgress,
                modifier = Modifier.fillMaxSize(),
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 32.dp)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(Res.string.app_name),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(Res.string.ui_home_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }

                StatusCard(
                    enabled = enabled,
                    onToggle = onToggleEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatusCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotion.current
    val spacing = MaterialTheme.spacing

    val containerColor by animateColorAsState(
        targetValue = if (enabled) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(motion.durationMedium, easing = FastOutSlowInEasing),
        label = "containerColor",
    )
    val iconContainerColor by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.errorContainer,
        animationSpec = tween(motion.durationMedium),
        label = "iconBg",
    )
    val iconTint by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.error,
        animationSpec = tween(motion.durationMedium),
        label = "iconTint",
    )
    val iconRingColor by animateColorAsState(
        targetValue = if (enabled) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(motion.durationMedium),
        label = "iconRingColor",
    )
    val ringRevealProgress by animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        animationSpec = tween(motion.durationMedium, easing = FastOutSlowInEasing),
        label = "ringRevealProgress",
    )
    val iconRingAlpha = 0.03f + (0.29f * ringRevealProgress)
    val morphProgress by animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        animationSpec = tween(motion.durationMedium, easing = FastOutSlowInEasing),
        label = "badgeMorph",
    )
    val cardMorphProgress by animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        animationSpec = tween(motion.durationMedium, easing = FastOutSlowInEasing),
        label = "cardMorph",
    )
    val iconBadgeScale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.98f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "iconBadgeScale",
    )
    val carrierRotation = remember { Animatable(0f) }
    LaunchedEffect(enabled, motion.durationSlow, motion.durationMedium) {
        if (enabled) {
            while (true) {
                carrierRotation.animateTo(
                    targetValue = carrierRotation.value + 360f,
                    animationSpec = tween(
                        durationMillis = motion.durationSlow * 18,
                        easing = LinearEasing,
                    ),
                )
                if (carrierRotation.value > 10000f) {
                    carrierRotation.snapTo(carrierRotation.value % 360f)
                }
            }
        } else {
            val normalized = ((carrierRotation.value % 360f) + 360f) % 360f
            val settleTarget = if (normalized > 180f) {
                carrierRotation.value + (360f - normalized)
            } else {
                carrierRotation.value - normalized
            }
            carrierRotation.animateTo(
                targetValue = settleTarget,
                animationSpec = tween(
                    durationMillis = motion.durationMedium,
                    easing = FastOutSlowInEasing,
                ),
            )
            carrierRotation.snapTo(0f)
        }
    }
    val sharedRotation = carrierRotation.value
    val badgeShape = rememberStatusBadgeShape(morphProgress)
    val cardShape = rememberStatusCardShape(cardMorphProgress)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "pressScale",
    )

    val enabledLabel =
        if (enabled) {
            stringResource(Res.string.status_enabled)
        } else {
            stringResource(Res.string.status_disabled)
        }
    val firewallLabel = stringResource(Res.string.menu_firewall)

    Surface(
        shape = cardShape,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shape = cardShape
                clip = true
            }
            .toggleable(
                value = enabled,
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Switch,
                onValueChange = onToggle,
            )
            .semantics {
                contentDescription = firewallLabel
                stateDescription = enabledLabel
            },
        color = containerColor,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.extraLarge, vertical = spacing.large),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(spacing.medium),
                ) {
                    Box(
                        modifier = Modifier.size(72.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            shape = badgeShape,
                            color = Color.Transparent,
                            border = BorderStroke(
                                1.5.dp,
                                iconRingColor.copy(alpha = iconRingAlpha)
                            ),
                            modifier = Modifier
                                .size(72.dp)
                                .graphicsLayer {
                                    rotationZ = sharedRotation
                                    shape = badgeShape
                                    clip = true
                                },
                        ) {}

                        Surface(
                            shape = badgeShape,
                            color = iconContainerColor,
                            modifier = Modifier
                                .size(60.dp)
                                .graphicsLayer {
                                    scaleX = iconBadgeScale
                                    scaleY = iconBadgeScale
                                    rotationZ = sharedRotation * 0.72f
                                    shape = badgeShape
                                    clip = true
                                },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = QuietGuardMark,
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .graphicsLayer { rotationZ = -(sharedRotation * 0.72f) },
                                )
                            }
                        }
                    }

                    Text(
                        text = enabledLabel,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )

                    // Switch
                    Switch(
                        checked = enabled,
                        onCheckedChange = null,
                    )
                }
            }
        }
    }
}
