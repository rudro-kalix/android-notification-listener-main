package ee.kytt.androidnotificationlistener.ui

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ee.kytt.androidnotificationlistener.Constants
import ee.kytt.androidnotificationlistener.R
import ee.kytt.androidnotificationlistener.ui.element.ContentGroup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onSettingsClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.open_settings))
        }

        Spacer(modifier = Modifier.height(32.dp))

        StatsGroup(context)
    }
}

@Composable
private fun StatsGroup(context: Context) {
    val prefs = context.getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
    val receivedCount = remember { mutableIntStateOf(prefs.getInt(Constants.PREF_RECEIVED_COUNT, 0)) }
    val syncedCount = remember { mutableIntStateOf(prefs.getInt(Constants.PREF_SYNCED_COUNT, 0)) }
    val failedCount = remember { mutableIntStateOf(prefs.getInt(Constants.PREF_FAILED_COUNT, 0)) }
    val lastSyncTime = remember { mutableLongStateOf(prefs.getLong(Constants.PREF_LAST_SYNC_TIME, 0L)) }

    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                Constants.PREF_RECEIVED_COUNT -> {
                    receivedCount.intValue = prefs.getInt(Constants.PREF_RECEIVED_COUNT, 0)
                }
                Constants.PREF_SYNCED_COUNT -> {
                    syncedCount.intValue = prefs.getInt(Constants.PREF_SYNCED_COUNT, 0)
                }
                Constants.PREF_FAILED_COUNT -> {
                    failedCount.intValue = prefs.getInt(Constants.PREF_FAILED_COUNT, 0)
                }
                Constants.PREF_LAST_SYNC_TIME -> {
                    lastSyncTime.longValue = prefs.getLong(Constants.PREF_LAST_SYNC_TIME, 0L)
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    ContentGroup(title = stringResource(R.string.sync_stats_title)) {
        StatLine(
            label = stringResource(R.string.notifications_received),
            value = receivedCount.intValue.toString()
        )
        StatLine(
            label = stringResource(R.string.notifications_synced),
            value = syncedCount.intValue.toString()
        )
        StatLine(
            label = stringResource(R.string.notifications_failed),
            value = failedCount.intValue.toString()
        )
        StatLine(
            label = stringResource(R.string.last_sync_time),
            value = formatLastSync(context, lastSyncTime.longValue)
        )
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
}

private fun formatLastSync(context: Context, timestamp: Long): String {
    if (timestamp <= 0L) {
        return context.getString(R.string.datetime_never)
    }
    val pattern = context.getString(R.string.datetime_format)
    val formatter = SimpleDateFormat(pattern, Locale.getDefault())
    return formatter.format(Date(timestamp))
}
