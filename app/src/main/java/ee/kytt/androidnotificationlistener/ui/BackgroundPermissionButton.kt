package ee.kytt.androidnotificationlistener.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.content.getSystemService
import ee.kytt.androidnotificationlistener.R
import ee.kytt.androidnotificationlistener.ui.element.SettingSwitch

@Composable
fun BackgroundPermissionButton(context: Context) {
    val currentContext by rememberUpdatedState(context)
    var isIgnoringOptimizations by remember {
        mutableStateOf(isIgnoringBatteryOptimizations(currentContext))
    }

    SettingSwitch(
        title = stringResource(R.string.allow_background_title),
        checked = isIgnoringOptimizations,
        description = stringResource(R.string.allow_background_description),
        onClick = {
            requestIgnoreBatteryOptimizations(currentContext)
            isIgnoringOptimizations = isIgnoringBatteryOptimizations(currentContext)
        }
    )
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService<PowerManager>() ?: return false
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun requestIgnoreBatteryOptimizations(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    context.startActivity(intent)
}
