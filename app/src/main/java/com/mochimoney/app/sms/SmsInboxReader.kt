package com.mochimoney.app.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat

class SmsInboxReader(private val context: Context) {
    fun hasReadSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

    fun readInbox(limit: Int = 500): List<RawSms> {
        if (!hasReadSmsPermission()) return emptyList()

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
        )
        val sortOrder = "${Telephony.Sms.DATE} DESC"

        return context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            null,
            null,
            sortOrder,
        )?.use { cursor ->
            val senderIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            buildList {
                while (cursor.moveToNext() && size < limit) {
                    add(
                        RawSms(
                            body = cursor.getString(bodyIndex).orEmpty(),
                            sender = cursor.getString(senderIndex),
                            receivedAtMillis = cursor.getLong(dateIndex),
                        ),
                    )
                }
            }
        }.orEmpty()
    }
}
