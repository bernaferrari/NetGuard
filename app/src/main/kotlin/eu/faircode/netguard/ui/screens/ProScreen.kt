package eu.faircode.netguard.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import eu.faircode.netguard.ActivityPro
import eu.faircode.netguard.R
import eu.faircode.netguard.ui.util.StatePlaceholder

@Composable
fun ProScreen() {
    val context = LocalContext.current
    StatePlaceholder(
        title = stringResource(R.string.title_pro),
        message = stringResource(R.string.ui_empty_pro_body),
        secondaryMessage = stringResource(R.string.ui_empty_pro_details),
        icon = Icons.Default.Shield,
        actionLabel = stringResource(R.string.menu_support),
        onAction = { context.startActivity(Intent(context, ActivityPro::class.java)) },
        secondaryActionLabel = stringResource(R.string.ui_learn_more),
        onSecondaryAction = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.netguard.me/#pro1"))
            context.startActivity(intent)
        },
    )
}
