package com.bernaferari.renetguard.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.runtime.Composable
import com.bernaferari.renetguard.platform.NetGuardPlatform
import com.bernaferari.renetguard.platform.openUrl
import com.bernaferari.renetguard.ui.util.StatePlaceholder
import netguard.shared.generated.resources.Res
import netguard.shared.generated.resources.menu_support
import netguard.shared.generated.resources.title_pro
import netguard.shared.generated.resources.ui_empty_pro_body
import netguard.shared.generated.resources.ui_empty_pro_details
import netguard.shared.generated.resources.ui_learn_more
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProScreen() {
    StatePlaceholder(
        title = stringResource(Res.string.title_pro),
        message = stringResource(Res.string.ui_empty_pro_body),
        secondaryMessage = stringResource(Res.string.ui_empty_pro_details),
        icon = Icons.Default.Shield,
        actionLabel = stringResource(Res.string.menu_support),
        onAction = { NetGuardPlatform.proFeatures.openProScreen() },
        secondaryActionLabel = stringResource(Res.string.ui_learn_more),
        onSecondaryAction = { openUrl("http://www.netguard.me/#pro1") },
    )
}