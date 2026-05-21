package com.mochimoney.app.data

import com.mochimoney.app.data.repository.InMemoryUpiTransactionRepository
import com.mochimoney.app.sms.RawSms
import com.mochimoney.app.sms.SmsParseResult
import com.mochimoney.app.sms.UpiSmsParser
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class InMemoryUpiTransactionRepositoryTest {
    private val parser = UpiSmsParser(zoneId = ZoneId.of("Asia/Kolkata"))

    @Test
    fun upsertKeepsSingleRecordForSameDedupeKey() {
        val repository = InMemoryUpiTransactionRepository()
        val parsed = parser.parse(
            RawSms("UPI A/C X5982 debited by 210.00 on date 07May26 trf to Cafe Coffee Day Refno 005155779681")
        ) as SmsParseResult.Parsed

        val first = repository.upsert(parsed.transaction)
        val second = repository.upsert(parsed.transaction.copy(id = 0L))

        assertEquals(first, second)
        assertEquals(1, repository.getAll().size)
    }
}
