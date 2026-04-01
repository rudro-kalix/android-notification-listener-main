package ee.kytt.androidnotificationlistener.service

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import ee.kytt.androidnotificationlistener.Constants
import org.json.JSONArray
import org.json.JSONObject

object NotificationSyncManager {

    private val queueLock = Any()

    fun handleNotification(context: Context, data: Map<String, Any?>) {
        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val syncEnabled = prefs.getBoolean(Constants.PREF_SYNC_ENABLED, true)

        if (syncEnabled) {
            uploadNow(context, data)
            syncNow(context)
        } else {
            enqueue(context, data)
            Log.d("FIREBASE", "Sync disabled. Queued notification for manual sync.")
        }
    }

    fun syncNow(context: Context) {
        val queue = getQueue(context)
        if (queue.isEmpty()) return

        queue.forEach { item ->
            uploadNow(context, item, removeFromQueueOnSuccess = true)
        }
    }

    private fun uploadNow(
        context: Context,
        data: Map<String, Any?>,
        removeFromQueueOnSuccess: Boolean = false
    ) {
        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val docId = data["docId"] as? String ?: return

        FirebaseFirestore.getInstance()
            .collection("notifications")
            .document(docId)
            .set(data.filterValues { it != null })
            .addOnSuccessListener {
                Log.d("FIREBASE", "Uploaded: $docId amount=${data["amount"]}")
                incrementCount(context, Constants.PREF_SYNCED_COUNT)
                prefs.edit()
                    .putLong(Constants.PREF_LAST_SYNC_TIME, System.currentTimeMillis())
                    .apply()
                if (removeFromQueueOnSuccess) {
                    dequeueById(context, docId)
                }
            }
            .addOnFailureListener { e ->
                Log.e("FIREBASE", "Upload failed", e)
                incrementCount(context, Constants.PREF_FAILED_COUNT)
            }
    }

    private fun enqueue(context: Context, data: Map<String, Any?>) {
        synchronized(queueLock) {
            val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            val existing = prefs.getString(Constants.PREF_PENDING_SYNC_ITEMS, null)
            val jsonArray = if (existing.isNullOrBlank()) JSONArray() else JSONArray(existing)
            jsonArray.put(JSONObject(data))
            prefs.edit().putString(Constants.PREF_PENDING_SYNC_ITEMS, jsonArray.toString()).apply()
        }
    }

    private fun dequeueById(context: Context, docId: String) {
        synchronized(queueLock) {
            val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            val existing = prefs.getString(Constants.PREF_PENDING_SYNC_ITEMS, null)
            if (existing.isNullOrBlank()) return

            val source = JSONArray(existing)
            val filtered = JSONArray()

            for (i in 0 until source.length()) {
                val item = source.optJSONObject(i) ?: continue
                if (item.optString("docId") != docId) {
                    filtered.put(item)
                }
            }

            prefs.edit().putString(Constants.PREF_PENDING_SYNC_ITEMS, filtered.toString()).apply()
        }
    }

    private fun getQueue(context: Context): List<Map<String, Any?>> {
        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val rawQueue = prefs.getString(Constants.PREF_PENDING_SYNC_ITEMS, null) ?: return emptyList()
        if (rawQueue.isBlank()) return emptyList()

        val queueJson = JSONArray(rawQueue)
        return buildList {
            for (i in 0 until queueJson.length()) {
                val item = queueJson.optJSONObject(i) ?: continue
                add(
                    mapOf(
                        "source" to item.optString("source"),
                        "docId" to item.optString("docId"),
                        "transactionId" to item.opt("transactionId")?.toString(),
                        "amount" to item.optDouble("amount").takeIf { !it.isNaN() },
                        "packageName" to item.optString("packageName"),
                        "title" to item.optString("title"),
                        "text" to item.optString("text"),
                        "time" to item.optLong("time")
                    )
                )
            }
        }
    }

    private fun incrementCount(context: Context, key: String) {
        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(key, 0)
        prefs.edit().putInt(key, current + 1).apply()
    }
}
