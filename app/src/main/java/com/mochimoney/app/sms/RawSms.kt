package com.mochimoney.app.sms

data class RawSms(
    val body: String,
    val sender: String? = null,
    val receivedAtMillis: Long? = null
)
