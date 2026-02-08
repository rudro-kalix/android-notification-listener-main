package ee.kytt.androidnotificationlistener

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import ee.kytt.androidnotificationlistener.ui.MainScreen
import ee.kytt.androidnotificationlistener.ui.SettingsScreen
import ee.kytt.androidnotificationlistener.ui.theme.AndroidNotificationListenerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AndroidNotificationListenerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ScreenHolder(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        // 🔐 Permission checks (AFTER UI is set)
        if (!hasNotificationAccess()) {
            requestNotificationAccess()
        }

        if (!hasSmsPermission()) {
            requestSmsPermission()
        }

    }

    // =========================
    // 🔐 PERMISSION HELPERS
    // =========================

    private fun hasSmsPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.READ_SMS) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun requestSmsPermission() {
        requestPermissions(
            arrayOf(
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS
            ),
            1001
        )
    }

    private fun hasNotificationAccess(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners?.contains(packageName) == true
    }

    private fun requestNotificationAccess() {
        startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        )
    }
}

/* =========================
   COMPOSE UI (UNCHANGED)
   ========================= */

@Preview(showBackground = true)
@Composable
fun NotificationAccessUIPreview() {
    AndroidNotificationListenerTheme {
        ScreenHolder()
    }
}

@Composable
fun ScreenHolder(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        BackHandler { showSettings = false }
        SettingsScreen(
            context = context,
            modifier = modifier,
            onBack = { showSettings = false }
        )
    } else {
        MainScreen(
            context = context,
            modifier = modifier,
            onSettingsClick = { showSettings = true }
        )
    }
}
