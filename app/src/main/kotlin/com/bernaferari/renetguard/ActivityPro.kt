package com.bernaferari.renetguard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.bernaferari.renetguard.ui.theme.NetGuardThemeFromPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActivityPro : ComponentActivity() {
    private var iab: IAB? = null
    private var refreshKey by mutableStateOf(0)
    private var availability by mutableStateOf<Map<String, Boolean>>(emptyMap())
    private var showChallenge by mutableStateOf(false)
    private var pendingSku: String? = null

    private val purchaseLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                pendingSku?.let { sku ->
                    IAB.setBought(sku, this)
                    refreshKey++
                }
            }
            pendingSku = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "Create")
        super.onCreate(savedInstanceState)

        setContent {
            NetGuardThemeFromPrefs {
                ProContent(
                    availability = availability,
                    refreshKey = refreshKey,
                    showChallenge = showChallenge,
                    onBuy = { sku, isDonation -> buySku(sku, isDonation) },
                    onChallenge = { showChallenge = true },
                    onDismissChallenge = { showChallenge = false },
                    onChallengeSuccess = {
                        IAB.setBought(SKU_DONATION, this)
                        refreshKey++
                        showChallenge = false
                    },
                )
            }
        }

        try {
            iab =
                IAB(
                    object : IAB.Delegate {
                        override fun onReady(iab: IAB) {
                            Log.i(TAG, "IAB ready")
                            try {
                                iab.updatePurchases()
                                refreshKey++
                                lifecycleScope.launch(Dispatchers.IO) {
                                    val map = HashMap<String, Boolean>()
                                    val skus =
                                        listOf(
                                            SKU_LOG,
                                            SKU_FILTER,
                                            SKU_NOTIFY,
                                            SKU_SPEED,
                                            SKU_THEME,
                                            SKU_PRO1,
                                            SKU_SUPPORT1,
                                            SKU_SUPPORT2,
                                        )
                                    for (sku in skus) {
                                        map[sku] =
                                            runCatching { iab.isAvailable(sku) }.getOrDefault(true)
                                    }
                                    withContext(Dispatchers.Main) {
                                        availability = map
                                    }
                                }
                            } catch (ex: Throwable) {
                                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
                            }
                        }
                    },
                    this,
                )
            iab?.bind()
        } catch (ex: Throwable) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
        }
    }

    override fun onDestroy() {
        Log.i(TAG, "Destroy")
        iab?.unbind()
        iab = null
        super.onDestroy()
    }

    private fun buySku(sku: String, isDonation: Boolean) {
        try {
            val iabInstance = iab ?: return
            val pi = iabInstance.getBuyIntent(sku, isDonation)
            if (pi != null) {
                pendingSku = sku
                val request = IntentSenderRequest.Builder(pi.intentSender).build()
                purchaseLauncher.launch(request)
            }
        } catch (ex: Throwable) {
            pendingSku = null
            Log.i(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex))
        }
    }

    companion object {
        private const val TAG = "NetGuard.Pro"

        const val SKU_LOG = "log"
        const val SKU_FILTER = "filter"
        const val SKU_NOTIFY = "notify"
        const val SKU_SPEED = "speed"
        const val SKU_THEME = "theme"
        const val SKU_PRO1 = "pro1"
        const val SKU_SUPPORT1 = "support1"
        const val SKU_SUPPORT2 = "support2"
        const val SKU_DONATION = "donation"

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProContent(
    availability: Map<String, Boolean>,
    refreshKey: Int,
    showChallenge: Boolean,
    onBuy: (String, Boolean) -> Unit,
    onChallenge: () -> Unit,
    onDismissChallenge: () -> Unit,
    onChallengeSuccess: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val items =
        listOf(
            ProItem(ActivityPro.SKU_LOG, R.string.title_pro_log, false),
            ProItem(ActivityPro.SKU_FILTER, R.string.title_pro_filter, false),
            ProItem(ActivityPro.SKU_NOTIFY, R.string.title_pro_notify, false),
            ProItem(ActivityPro.SKU_SPEED, R.string.title_pro_speed, false),
            ProItem(ActivityPro.SKU_THEME, R.string.title_pro_theme, false),
            ProItem(ActivityPro.SKU_PRO1, R.string.title_pro_all, false),
            ProItem(ActivityPro.SKU_SUPPORT1, R.string.title_pro_dev, true),
            ProItem(ActivityPro.SKU_SUPPORT2, R.string.title_pro_dev, true),
        )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_pro)) },
                actions = {
                    if (!Util.isPlayStoreInstall(context)) {
                        TextButton(onClick = onChallenge) {
                            Text(text = stringResource(R.string.title_pro_challenge))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.title_pro_description),
                style = MaterialTheme.typography.bodyMedium,
            )

            items.forEach { item ->
                val purchased = remember(refreshKey) { IAB.isPurchased(item.sku, context) }
                val available = availability[item.sku] ?: true
                ProRow(
                    title = stringResource(item.titleRes),
                    purchased = purchased,
                    available = available,
                    onClick = {
                        val link = "http://www.netguard.me/#${item.sku}".toUri()
                        val intent = Intent(Intent.ACTION_VIEW, link)
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        }
                    },
                    onBuy = { onBuy(item.sku, item.isDonation) },
                )
            }
        }
    }

    if (showChallenge) {
        val androidId =
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val challenge = "O3$androidId"
        val seed = "NetGuard3"
        ChallengeDialog(
            challenge = challenge,
            seed = seed,
            onDismiss = onDismissChallenge,
            onSuccess = onChallengeSuccess,
        )
    }
}

@Composable
private fun ProRow(
    title: String,
    purchased: Boolean,
    available: Boolean,
    onClick: () -> Unit,
    onBuy: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        when {
            purchased -> Text(
                text = stringResource(R.string.title_pro_bought),
                style = MaterialTheme.typography.bodyMedium
            )

            !available -> Text(text = stringResource(R.string.title_pro_unavailable))
            else -> FilledTonalButton(onClick = onBuy) {
                Text(text = stringResource(R.string.title_pro_buy))
            }
        }
    }
}

@Composable
private fun ChallengeDialog(
    challenge: String,
    seed: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val challengeLabel = stringResource(R.string.title_pro_challenge)
    var response by remember { mutableStateOf("") }
    val expected =
        remember(challenge, seed) { runCatching { Util.md5(challenge, seed) }.getOrDefault("") }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (response.uppercase() == expected) {
                        onSuccess()
                    }
                },
            ) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        },
        title = { Text(text = stringResource(R.string.title_pro_challenge)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = challenge, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = {
                            val clip = ClipData.newPlainText(
                                challengeLabel,
                                challenge,
                            )
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, android.R.string.copy, Toast.LENGTH_LONG).show()
                        },
                    ) {
                        Text(text = stringResource(android.R.string.copy))
                    }
                    FilledTonalButton(
                        onClick = {
                            val item = clipboard.primaryClip?.getItemAt(0)
                            response = item?.text?.toString().orEmpty()
                        },
                    ) {
                        Text(text = stringResource(android.R.string.paste))
                    }
                }
                OutlinedTextField(
                    value = response,
                    onValueChange = { response = it },
                    label = { Text(text = stringResource(R.string.title_pro_reponse)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

private data class ProItem(
    val sku: String,
    val titleRes: Int,
    val isDonation: Boolean,
)
