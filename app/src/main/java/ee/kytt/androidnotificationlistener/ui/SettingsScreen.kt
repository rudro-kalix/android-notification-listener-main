package ee.kytt.androidnotificationlistener.ui

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ee.kytt.androidnotificationlistener.Constants
import ee.kytt.androidnotificationlistener.Constants.PREF_PACKAGE_PATTERN
import ee.kytt.androidnotificationlistener.R
import ee.kytt.androidnotificationlistener.service.NotificationSyncManager
import ee.kytt.androidnotificationlistener.ui.element.ContentGroup
import ee.kytt.androidnotificationlistener.ui.element.SettingSwitch
import ee.kytt.androidnotificationlistener.ui.element.TextField

@Composable
fun SettingsScreen(
    context: Context,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val divModifier = Modifier.padding(vertical = 16.dp)
    val prefs = remember { context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE) }
    var syncEnabled by remember { mutableStateOf(prefs.getBoolean(Constants.PREF_SYNC_ENABLED, true)) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { onBack?.invoke() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium)
        }

        Spacer(modifier = Modifier.height(32.dp))

        ContentGroup(title = stringResource(R.string.notifications)) {
            EnableNotificationsButton(context)

            HorizontalDivider(modifier = divModifier)

            AutoRevokeStatusButton(context)

            HorizontalDivider(modifier = divModifier)

            BackgroundPermissionButton(context)
        }

        ContentGroup(title = stringResource(R.string.sync_options_title)) {
            SettingSwitch(
                title = stringResource(R.string.sync_enabled_title),
                checked = syncEnabled,
                description = stringResource(R.string.sync_enabled_description)
            ) { enabled ->
                syncEnabled = enabled
                prefs.edit().putBoolean(Constants.PREF_SYNC_ENABLED, enabled).apply()
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { NotificationSyncManager.syncNow(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.sync_now))
            }
        }

        ContentGroup(title = stringResource(R.string.filter)) {
            TextField(
                context = context,
                prefKey = PREF_PACKAGE_PATTERN,
                labelSet = stringResource(R.string.monitor_matching_apps),
                labelNotSet = stringResource(R.string.monitor_all_apps),
                description = stringResource(R.string.monitor_matching_apps_description)
            )
        }

    }
}
