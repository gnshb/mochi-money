package com.mochimoney.app.sms

import com.mochimoney.app.domain.model.DefaultCategoryIds
import com.mochimoney.app.domain.model.TransactionDirection
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpiSmsParserTest {
    private val parser = UpiSmsParser(zoneId = ZoneId.of("Asia/Kolkata"))

    @Test
    fun parsesSampleSbiSms() {
        val body = "Dear UPI user A/C X5982 debited by 210.00 on date 07May26 trf to Cafe Coffee Day Refno 005155779681 If not u? call-1800111109 for other services-18001234-SBI"

        val result = parser.parse(RawSms(body = body, sender = "SBI"))

        assertTrue(result is SmsParseResult.Parsed)
        val transaction = (result as SmsParseResult.Parsed).transaction
        assertEquals(TransactionDirection.DEBIT, transaction.direction)
        assertEquals(21_000L, transaction.amountPaise)
        assertEquals(LocalDate.of(2026, 5, 7), transaction.occurredOn)
        assertEquals("Cafe Coffee Day", transaction.counterparty)
        assertEquals("005155779681", transaction.referenceNumber)
        assertEquals("5982", transaction.accountSuffix)
        assertEquals(DefaultCategoryIds.FOOD, transaction.categoryId)
    }

    @Test
    fun parsesDifferentMerchantWithoutCcdAssumption() {
        val body = "UPI user A/C XX4321 debited by Rs.1,250.50 on date 08May26 trf to Green Super Market Refno 999888777666"

        val result = parser.parse(RawSms(body = body))

        assertTrue(result is SmsParseResult.Parsed)
        val transaction = (result as SmsParseResult.Parsed).transaction
        assertEquals("Green Super Market", transaction.counterparty)
        assertEquals(125_050L, transaction.amountPaise)
        assertEquals(DefaultCategoryIds.GROCERIES, transaction.categoryId)
    }

    @Test
    fun unknownMerchantNeedsUserCategorization() {
        val body = "UPI user A/C XX4321 debited by Rs.100.00 on date 08May26 trf to New Payee Refno 999888777667"

        val result = parser.parse(RawSms(body = body))

        assertTrue(result is SmsParseResult.Parsed)
        val transaction = (result as SmsParseResult.Parsed).transaction
        assertEquals("New Payee", transaction.counterparty)
        assertEquals(DefaultCategoryIds.UNCATEGORIZED, transaction.categoryId)
    }

    @Test
    fun ignoresOtpMessages() {
        val result = parser.parse(RawSms(body = "123456 is your UPI OTP. Do not share it."))

        assertTrue(result is SmsParseResult.Ignored)
    }

    @Test
    fun dedupeKeyIsStableAcrossWhitespace() {
        val compact = "UPI A/C X5982 debited by 210.00 on date 07May26 trf to Cafe Coffee Day Refno 005155779681"
        val spaced = "UPI   A/C X5982 debited by 210.00 on date 07May26   trf to   Cafe Coffee Day   Refno 005155779681"

        val first = (parser.parse(RawSms(body = compact)) as SmsParseResult.Parsed).transaction
        val second = (parser.parse(RawSms(body = spaced)) as SmsParseResult.Parsed).transaction

        assertEquals(first.dedupeKey, second.dedupeKey)
    }

    @Test
    fun distinctReferenceProducesDifferentDedupeKey() {
        val first = (parser.parse(RawSms(body = "UPI A/C X5982 debited by 210.00 on date 07May26 trf to Cafe Coffee Day Refno 005155779681")) as SmsParseResult.Parsed).transaction
        val second = (parser.parse(RawSms(body = "UPI A/C X5982 debited by 210.00 on date 07May26 trf to Cafe Coffee Day Refno 005155779682")) as SmsParseResult.Parsed).transaction

        assertNotEquals(first.dedupeKey, second.dedupeKey)
    }
}
