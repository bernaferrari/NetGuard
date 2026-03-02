package com.bernaferari.renetguard.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.graphics.shapes.Morph
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferari.renetguard.R
import com.bernaferari.renetguard.Util
import com.bernaferari.renetguard.ui.theme.LocalMotion
import com.bernaferari.renetguard.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onToggleEnabled: (Boolean) -> Unit,
    onOpenFirewall: (AppsFilter) -> Unit = {},
    onOpenLogs: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val notificationManager = remember(context) { NotificationManagerCompat.from(context) }
    var notificationsEnabled by remember {
        mutableStateOf(notificationManager.areNotificationsEnabled() && Util.canNotify(context))
    }
    var hasRequestedNotificationPermission by rememberSaveable { mutableStateOf(false) }
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            hasRequestedNotificationPermission = true
            notificationsEnabled = isGranted && notificationManager.areNotificationsEnabled()
        }
    val canRequestNotificationPermission =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED

    DisposableEffect(lifecycleOwner, context, notificationManager) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    notificationsEnabled =
                        notificationManager.areNotificationsEnabled() && Util.canNotify(context)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val enabled by viewModel.enabled.collectAsStateWithLifecycle()
    val rulesUiState by viewModel.rulesUiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.ensureRulesLoaded()
    }

    val rules = rulesUiState.rules
    val blockedApps = rules.count { it.wifi_blocked || it.other_blocked }
    val allowedApps = rules.count { !it.wifi_blocked && !it.other_blocked }
    val totalApps = rules.size

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(spacing.large),
            verticalArrangement = Arrangement.spacedBy(spacing.large),
        ) {

            StatusCard(
                enabled = enabled,
                onToggle = onToggleEnabled,
            )

            if (!notificationsEnabled) {
                NotificationPermissionCard(
                    canRequestPermission = canRequestNotificationPermission && !hasRequestedNotificationPermission,
                    onRequestPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onOpenSettings = {
                        val intent =
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                        context.startActivity(intent)
                    },
                )
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.stat_blocked_today),
                    value = blockedApps,
                    icon = Icons.Default.Block,
                    tint = MaterialTheme.colorScheme.error,
                    onClick = { onOpenFirewall(AppsFilter.Blocked) },
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.stat_allowed_today),
                    value = allowedApps,
                    icon = Icons.Default.CheckCircle,
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = { onOpenFirewall(AppsFilter.Allowed) },
                )
            }

            StatCard(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.stat_active_rules),
                value = totalApps,
                icon = Icons.Default.Tune,
                tint = MaterialTheme.colorScheme.tertiary,
                emphasized = true,
                onClick = { onOpenFirewall(AppsFilter.All) },
            )
        }
    }
}

@Composable
private fun NotificationPermissionCard(
    canRequestPermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val actionLabel =
        if (canRequestPermission) {
            stringResource(R.string.action_enable)
        } else {
            stringResource(R.string.menu_settings)
        }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            Text(
                text = stringResource(R.string.channel_access),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.msg_notifications),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(
                onClick = {
                    if (canRequestPermission) {
                        onRequestPermission()
                    } else {
                        onOpenSettings()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = if (canRequestPermission) Icons.Default.Notifications else Icons.Default.Settings,
                    contentDescription = null,
                )
                Box(modifier = Modifier.size(spacing.extraSmall))
                Text(text = actionLabel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatusCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
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
    val shaderColorProgress by animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        animationSpec = tween(
            durationMillis = motion.durationSlow * 2,
            easing = FastOutSlowInEasing
        ),
        label = "shaderColorProgress",
    )
    val triggerToggle: (Boolean) -> Unit = { next ->
        onToggle(next)
    }
    val badgeMorph =
        remember { Morph(start = MaterialShapes.Square, end = MaterialShapes.Cookie9Sided) }
    val badgeShape = rememberMorphShape(morph = badgeMorph, progress = morphProgress)
    val cardMorph = remember { Morph(start = MaterialShapes.Square, end = MaterialShapes.Gem) }
    val cardShape = rememberMorphShape(morph = cardMorph, progress = cardMorphProgress)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "pressScale",
    )

    val statusDescription = if (enabled) {
        stringResource(R.string.status_enabled)
    } else {
        stringResource(R.string.status_disabled)
    }

    Surface(
        onClick = { triggerToggle(!enabled) },
        shape = cardShape,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .semantics { contentDescription = statusDescription },
        interactionSource = interactionSource,
        color = containerColor,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                FirewallStateShader(
                    enabledProgress = shaderColorProgress,
                    modifier = Modifier.matchParentSize(),
                )
            }

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
                                .graphicsLayer { rotationZ = sharedRotation },
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
                                },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Security,
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
                        text = if (enabled) {
                            stringResource(R.string.status_enabled)
                        } else {
                            stringResource(R.string.status_disabled)
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )

                    // Switch
                    Switch(
                        checked = enabled,
                        onCheckedChange = triggerToggle,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun rememberMorphShape(
    morph: Morph,
    progress: Float,
): Shape =
    remember(morph, progress) {
        object : Shape {
            override fun createOutline(
                size: androidx.compose.ui.geometry.Size,
                layoutDirection: LayoutDirection,
                density: Density,
            ): Outline {
                val path = morph.toPath(progress = progress, startAngle = 0)
                val scaleMatrix = Matrix().apply { scale(x = size.width, y = size.height) }
                path.transform(scaleMatrix)

                val bounds = path.getBounds()
                val translateX = (size.width / 2f) - bounds.center.x
                val translateY = (size.height / 2f) - bounds.center.y
                path.transform(
                    Matrix().apply {
                        translate(x = translateX, y = translateY)
                    },
                )

                return Outline.Generic(path)
            }
        }
    }

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: Int,
    icon: ImageVector,
    tint: Color,
    emphasized: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val motion = LocalMotion.current
    val spacing = MaterialTheme.spacing

    val animatedValue by animateIntAsState(
        targetValue = value,
        animationSpec = tween(motion.durationSlow, easing = FastOutSlowInEasing),
        label = "statValue",
    )

    val containerColor = if (emphasized) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    val onContainer = if (emphasized) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val cardContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
        ) {
            // Tinted circular icon badge — use onContainer colour for contrast when emphasized
            val iconTint = if (emphasized) onContainer else tint
            val iconBg =
                if (emphasized) onContainer.copy(alpha = 0.15f) else tint.copy(alpha = 0.15f)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp),
                )
            }

            Text(
                text = animatedValue.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = onContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            cardContent()
        }
    } else {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            cardContent()
        }
    }
}
