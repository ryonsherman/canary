package com.canary.service

import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable

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
                android.app.PendingIntent.FLAG_MUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
            val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED))
            val techLists = arrayOf(arrayOf(Ndef::class.java.name))
            adapter.enableForegroundDispatch(activity, pendingIntent, filters, techLists)
        }
    }

    fun disableForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(activity)
    }

    fun readTagHash(intent: Intent): ByteArray? {
        return try {
            val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) ?: return null
            val ndef = Ndef.get(tag) ?: return null

            val message = ndef.cachedNdefMessage
            if (message != null) {
                val records = message.records
                if (records.isNotEmpty()) {
                    records[0].payload
                } else null
            } else {
                ndef.connect()
                val msg = ndef.ndefMessage
                ndef.close()
                msg?.records?.firstOrNull()?.payload
            }
        } catch (e: Exception) {
            null
        }
    }

    fun writeTagSecret(tag: Tag, secret: ByteArray): Boolean {
        return try {
            val record = NdefRecord.createExternal("com.canary", "tag", secret)
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

            false
        } catch (e: Exception) {
            false
        }
    }

    fun formatTagSecretForDisplay(secret: ByteArray): String {
        return secret.joinToString("") { "%02x".format(it) }
    }
}
