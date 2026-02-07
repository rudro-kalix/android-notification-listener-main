package ee.kytt.androidnotificationlistener.service

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import ee.kytt.androidnotificationlistener.Constants
import ee.kytt.androidnotificationlistener.R
import ee.kytt.androidnotificationlistener.data.Notification
import ee.kytt.androidnotificationlistener.data.SyncResult
import ee.kytt.androidnotificationlistener.persistence.NotificationDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.regex.Pattern

class NotificationListener : NotificationListenerService() {

    private val callbackService = ExternalCallback()

    // =====================
    // TRANSACTION ID
    // =====================
    private fun extractTransactionId(text: String?): String? {
        if (text.isNullOrBlank()) return null

        val pattern = Pattern.compile(
            "(TXNID|TRXID|TXN|Transaction|Ref)[^A-Za-z0-9]*([A-Za-z0-9]{6,})",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = pattern.matcher(text)
        return if (matcher.find()) matcher.group(2) else null
    }

    // =====================
    // AMOUNT (bKash / Nagad / Rocket)
    // =====================
    private fun extractReceivingAmount(text: String?): Double? {
        if (text.isNullOrBlank()) return null

        // Nagad / Rocket
        val pattern1 = Pattern.compile(
            "Amount\\s*:\\s*Tk\\s*([0-9]+(\\.[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE
        )
        val m1 = pattern1.matcher(text)
        if (m1.find()) {
            return m1.group(1).toDouble()
        }

        // bKash
        val pattern2 = Pattern.compile(
            "received\\s+Tk\\s*([0-9]+(\\.[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE
        )
        val m2 = pattern2.matcher(text)
        if (m2.find()) {
            return m2.group(1).toDouble()
        }

        return null
    }

    // =====================
    // MAIN LISTENER
    // =====================
    override fun onNotificationPosted(sbn: StatusBarNotification) {

        val notification = Notification(sbn)
        val context = applicationContext
        val prefs = context.getSharedPreferences(
            Constants.PREFS_NAME,
            MODE_PRIVATE
        )

        // Package filter
        val packagePattern =
            prefs.getString(Constants.PREF_PACKAGE_PATTERN, null) ?: ""

        if (!shouldMatch(packagePattern, notification.packageName)) {
            Log.d("NotificationListener", "Ignored package: ${notification.packageName}")
            return
        }

        // =====================
        // FIRESTORE
        // =====================
        val firestore = FirebaseFirestore.getInstance()

        val transactionId = extractTransactionId(notification.text)
        val amount = extractReceivingAmount(notification.text)
        val docId = transactionId ?: UUID.randomUUID().toString()

        val data = hashMapOf(
            "source" to "notification",
            "docId" to docId,
            "transactionId" to transactionId,
            "amount" to amount,
            "packageName" to notification.packageName,
            "title" to notification.title,
            "text" to notification.text,
            "time" to notification.time
        )

        firestore.collection("notifications")
            .document(docId)
            .set(data)
            .addOnSuccessListener {
                Log.d("FIREBASE", "Uploaded: $docId amount=$amount")
            }
            .addOnFailureListener { e ->
                Log.e("FIREBASE", "Upload failed", e)
            }

        // =====================
        // LEGACY CALLBACK
        // =====================
        val url = prefs.getString(Constants.PREF_CALLBACK_URL, null)
        val token = prefs.getString(Constants.PREF_CALLBACK_TOKEN, null)
        val notSetText = context.getString(R.string.callback_url_not_set)

        Log.d("NotificationListener", Json.Default.encodeToString(notification))

        if (url.isNullOrEmpty()) {
            saveFailedNotification(
                notification,
                context,
                SyncResult(false, "Failed", notSetText)
            )
            return
        }

        callbackService.sendAsync(url, token ?: "", notification) { result ->
            if (!result.success) {
                saveFailedNotification(notification, context, result)
            } else {
                saveSyncedNotification(notification, context)
                prefs.edit().apply {
                    putString(
                        Constants.PREF_LATEST_TITLE,
                        notification.description()
                    )
                    putString(
                        Constants.PREF_LATEST_PACKAGE_NAME,
                        notification.packageName
                    )
                    putString(
                        Constants.PREF_LATEST_STATUS,
                        result.status
                    )
                    putString(
                        Constants.PREF_LATEST_SYNC_ERROR,
                        result.userMessage
                    )
                    putLong(
                        Constants.PREF_LATEST_ATTEMPT_TIME,
                        System.currentTimeMillis()
                    )
                    putLong(
                        Constants.PREF_LAST_SUCCESS_TIME,
                        System.currentTimeMillis()
                    )
                    apply()
                }
            }
        }
    }

    // =====================
    // DATABASE HELPERS
    // =====================
    private fun saveFailedNotification(
        notification: Notification,
        context: Context,
        result: SyncResult
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = NotificationDatabase.getDatabase(context)
            db.notificationDao().insert(notification)
            val failed = db.notificationDao().countFailed()

            context.getSharedPreferences(
                Constants.PREFS_NAME,
                MODE_PRIVATE
            ).edit().apply {
                putInt(Constants.PREF_FAIL_COUNT, failed)
                putString(Constants.PREF_LATEST_STATUS, result.status)
                putString(Constants.PREF_LATEST_SYNC_ERROR, result.userMessage)
                putLong(
                    Constants.PREF_LATEST_ATTEMPT_TIME,
                    System.currentTimeMillis()
                )
                apply()
            }
        }
    }

    private fun saveSyncedNotification(
        notification: Notification,
        context: Context
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = NotificationDatabase.getDatabase(context)
            db.notificationDao().insert(
                notification.copy(synchronized = true)
            )
        }
    }

    private fun shouldMatch(packagePattern: String, name: String): Boolean {
        if (packagePattern.isBlank()) return true
        return try {
            Regex(packagePattern).containsMatchIn(name)
        } catch (e: Exception) {
            Log.w("NotificationListener", "Invalid regex pattern", e)
            false
        }
    }
}
