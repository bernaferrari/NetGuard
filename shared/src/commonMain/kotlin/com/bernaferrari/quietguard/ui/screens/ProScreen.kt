package com.bernaferrari.quietguard.ui.screens

import com.bernaferrari.quietguard.ui.icons.MaterialSymbols



import androidx.compose.runtime.Composable
import com.bernaferrari.quietguard.platform.NetGuardPlatform
import com.bernaferrari.quietguard.platform.openUrl
import com.bernaferrari.quietguard.ui.util.StatePlaceholder
import com.bernaferrari.quietguard.generated.resources.Res
import com.bernaferrari.quietguard.generated.resources.menu_support
import com.bernaferrari.quietguard.generated.resources.title_pro
import com.bernaferrari.quietguard.generated.resources.ui_empty_pro_body
import com.bernaferrari.quietguard.generated.resources.ui_empty_pro_details
import com.bernaferrari.quietguard.generated.resources.ui_learn_more
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProScreen() {
    StatePlaceholder(
        title = stringResource(Res.string.title_pro),
        message = stringResource(Res.string.ui_empty_pro_body),
        secondaryMessage = stringResource(Res.string.ui_empty_pro_details),
        icon = MaterialSymbols.Filled.Shield,
        actionLabel = stringResource(Res.string.menu_support),
        onAction = { NetGuardPlatform.proFeatures.openProScreen() },
        secondaryActionLabel = stringResource(Res.string.ui_learn_more),
        onSecondaryAction = { openUrl("http://www.netguard.me/#pro1") },
    )
}
