package ee.kytt.androidnotificationlistener.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.concurrent.futures.await
import androidx.core.content.PackageManagerCompat
import androidx.core.content.UnusedAppRestrictionsConstants
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import ee.kytt.androidnotificationlistener.R
import ee.kytt.androidnotificationlistener.ui.element.SettingSwitch
import kotlinx.coroutines.launch

@Composable
fun AutoRevokeStatusButton(context: Context) {
    val lifecycleOwner = LocalLifecycleOwner.current

    var autoRevokeStatus by remember { mutableIntStateOf(UnusedAppRestrictionsConstants.DISABLED) }
    val currentContext by rememberUpdatedState(context)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                lifecycleOwner.lifecycleScope.launch {
                    autoRevokeStatus = getAutoRevokeStatus(currentContext)
                }
            }
        }
        val lifecycle = lifecycleOwner.lifecycle
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        lifecycleOwner.lifecycleScope.launch {
            autoRevokeStatus = getAutoRevokeStatus(currentContext)
        }
        onDispose { }
    }

    val isEnabled = autoRevokeStatus != UnusedAppRestrictionsConstants.DISABLED

    SettingSwitch(
        title = stringResource(R.string.auto_revoke_permissions_title),
        checked = isEnabled,
        description = stringResource(R.string.auto_revoke_permissions_description),
        inverted = true,
        onClick = {
            openAppInfoSettings(context)
        }
    )
}

private suspend fun getAutoRevokeStatus(context: Context): Int {
    return try {
        val status = PackageManagerCompat.getUnusedAppRestrictionsStatus(context).await()
        Log.d("AutoRevokeStatusButton", "Auto-revoke status: $status")
        status
    } catch (e: Exception) {
        Log.w("AutoRevokeStatusButton", "Failed to get auto-revoke status", e)
        UnusedAppRestrictionsConstants.ERROR
    }
}

fun openAppInfoSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}
