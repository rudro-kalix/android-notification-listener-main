package ee.kytt.androidnotificationlistener.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import java.security.MessageDigest
import java.util.regex.Pattern

class SmsReceiver : BroadcastReceiver() {

    private val TRUSTED_SENDERS = listOf(
        "BKASH",
        "NAGAD",
        "ROCKET"
    )

    private val REQUIRED_SMS_PACKAGE = "com.google.android.apps.messaging"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(context)
        if (defaultSmsApp != REQUIRED_SMS_PACKAGE) {
            Log.w("SMS_BLOCK", "Blocked: default SMS app = $defaultSmsApp")
            return
        }

        val bundle = intent.extras ?: return
        val pdus = bundle["pdus"] as? Array<*> ?: return

        for (pdu in pdus) {
            val sms = SmsMessage.createFromPdu(pdu as ByteArray)

            val senderRaw = sms.originatingAddress ?: continue
            val sender = senderRaw.uppercase()
            val body = sms.messageBody ?: continue
            val timestamp = sms.timestampMillis

            // ❌ Block personal numbers
            if (sender.startsWith("+") || sender.any { it.isDigit() }) {
                Log.w("SMS_BLOCK", "Numeric sender blocked: $senderRaw")
                continue
            }

            // ❌ Block untrusted sender IDs
            if (TRUSTED_SENDERS.none { sender.contains(it) }) {
                Log.w("SMS_BLOCK", "Untrusted sender: $senderRaw")
                continue
            }

            // ✅ Extract RECEIVED amount (FIXED)
            val amount = extractReceivingAmount(body)
            if (amount == null) {
                Log.w("SMS_BLOCK", "Receiving amount not found")
                continue
            }

            val transactionId = extractTransactionId(body)
            val docId = generateUniqueId(sender, body, timestamp)

            val data = hashMapOf(
                "source" to "sms",
                "provider" to senderRaw,
                "amount" to amount,
                "transactionId" to transactionId,
                "text" to body,
                "timestamp" to timestamp,
                "smsApp" to defaultSmsApp
            )

            FirebaseFirestore.getInstance()
                .collection("transactions")
                .document(docId)
                .set(data)
                .addOnSuccessListener {
                    Log.d("FIREBASE_OK", "Saved transaction: $docId")
                }
                .addOnFailureListener { e ->
                    Log.e("FIREBASE_ERR", "Upload failed", e)
                }
        }
    }

    /**
     * MATCHES EXACT FORMATS:
     *
     * NAGAD / ROCKET:
     *   Amount: Tk 150.00
     *
     * BKASH:
     *   received Tk 270.00
     */
    private fun extractReceivingAmount(text: String): Double? {

        // 1️⃣ NAGAD / ROCKET
        val nagadRocketPattern = Pattern.compile(
            "AMOUNT\\s*:\\s*TK\\s*([0-9]+(\\.[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE
        )
        val nagadMatcher = nagadRocketPattern.matcher(text)
        if (nagadMatcher.find()) {
            return nagadMatcher.group(1).toDouble()
        }

        // 2️⃣ BKASH
        val bkashPattern = Pattern.compile(
            "RECEIVED\\s+TK\\s*([0-9]+(\\.[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE
        )
        val bkashMatcher = bkashPattern.matcher(text)
        if (bkashMatcher.find()) {
            return bkashMatcher.group(1).toDouble()
        }

        return null
    }

    private fun extractTransactionId(text: String): String? {
        val pattern = Pattern.compile(
            "(TXNID|TRXID)\\s*:?\\s*([A-Z0-9]{6,})",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = pattern.matcher(text)
        return if (matcher.find()) matcher.group(2) else null
    }

    private fun generateUniqueId(sender: String, body: String, time: Long): String {
        val input = "$sender|$body|$time"
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}