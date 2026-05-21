package com.mochimoney.app.sms

import com.mochimoney.app.domain.model.UpiTransaction

sealed class SmsParseResult {
    data class Parsed(val transaction: UpiTransaction) : SmsParseResult()
    data class Ignored(val reason: String) : SmsParseResult()
    data class Failed(val reason: String, val rawSms: RawSms) : SmsParseResult()
}
