package com.canary.service

import android.app.Activity
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class NfcService(private val activity: Activity) {

    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    val isNfcAvailable: Boolean get() = nfcAdapter != null

    val isNfcEnabled: Boolean get() = nfcAdapter?.isEnabled ?: false

    fun enableForegroundDispatch() {
        nfcAdapter?.let { adapter ->
            val pendingIntent = android.app.PendingIntent.getActivity(
                activity,
                0,
                Intent(activity, activity.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
            adapter.enableForegroundDispatch(activity, pendingIntent, null, null)
        }
    }

    fun disableForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(activity)
    }

    fun readTagHash(intent: Intent): ByteArray? {
        val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) ?: return null
        val ndef = Ndef.get(tag) ?: return null

        val message = ndef.cachedNdefMessage ?: return null
        val records = message.records
        if (records.isEmpty()) return null

        return records[0].payload
    }

    fun writeTagSecret(tag: Tag, secret: ByteArray): Boolean {
        val hash = MessageDigest.getInstance("SHA-256").digest(secret)
        val record = NdefRecord.createMime(
            "application/com.canary.tag",
            hash
        )
        val message = NdefMessage(arrayOf(record))

        val ndef = Ndef.get(tag)
        if (ndef != null) {
            ndef.connect()
            ndef.writeNdefMessage(message)
            ndef.close()
            return true
        }

        val formatable = NdefFormatable.get(tag)
        if (formatable != null) {
            formatable.connect()
            formatable.format(message)
            formatable.close()
            return true
        }

        return false
    }

    fun formatTagSecretForDisplay(secret: ByteArray): String {
        return secret.joinToString("") { "%02x".format(it) }
    }
}
