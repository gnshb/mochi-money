package com.mochimoney.app.sms

import com.mochimoney.app.data.repository.DefaultCategoryRepository
import com.mochimoney.app.domain.model.DedupeKeyGenerator
import com.mochimoney.app.domain.model.DefaultCategoryIds
import com.mochimoney.app.domain.model.TransactionDirection
import com.mochimoney.app.domain.model.UpiTransaction
import com.mochimoney.app.domain.repository.CategoryRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.util.Locale

class UpiSmsParser(
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val categoryRepository: CategoryRepository = DefaultCategoryRepository(),
    private val trustSender: Boolean = false,
) {
    fun parse(rawSms: RawSms): SmsParseResult {
        val body = rawSms.body.normalizedSmsBody()
        if (body.isBlank()) return SmsParseResult.Ignored("empty_sms")
        if (otpPattern.containsMatchIn(body)) return SmsParseResult.Ignored("otp_sms")
        if (!trustSender && !body.isUpiLike()) return SmsParseResult.Ignored("not_upi_sms")

        val direction = body.extractDirection()
            ?: return SmsParseResult.Ignored("missing_direction")
        val amountPaise = body.extractAmountPaise()
            ?: return SmsParseResult.Failed("missing_amount", rawSms)
        val occurredOn = body.extractOccurredOn()
            ?: rawSms.receivedAtMillis?.toLocalDate(zoneId)
            ?: return SmsParseResult.Failed("missing_date", rawSms)
        val referenceNumber = body.extractReferenceNumber()
        val accountSuffix = body.extractAccountSuffix()
        val counterparty = body.extractCounterparty(direction)
        val dedupeKey = DedupeKeyGenerator.generate(
            direction = direction,
            amountPaise = amountPaise,
            occurredOn = occurredOn,
            referenceNumber = referenceNumber,
            accountSuffix = accountSuffix,
            counterparty = counterparty
        )

        val uncategorized = UpiTransaction(
            dedupeKey = dedupeKey,
            direction = direction,
            amountPaise = amountPaise,
            occurredOn = occurredOn,
            counterparty = counterparty,
            referenceNumber = referenceNumber,
            accountSuffix = accountSuffix,
            sender = rawSms.sender,
            smsBodyHash = DedupeKeyGenerator.sha256(rawSms.body.normalizedSmsBody()),
            smsBody = rawSms.body,
            smsReceivedAtMillis = rawSms.receivedAtMillis,
            categoryId = DefaultCategoryIds.UNCATEGORIZED
        )

        return SmsParseResult.Parsed(
            uncategorized.copy(categoryId = categoryRepository.suggestFor(uncategorized).id)
        )
    }

    fun parseAll(rawMessages: Iterable<RawSms>): List<UpiTransaction> =
        rawMessages
            .mapNotNull { (parse(it) as? SmsParseResult.Parsed)?.transaction }
            .distinctBy { it.dedupeKey }

    private fun String.normalizedSmsBody(): String =
        trim().replace(Regex("\\s+"), " ")

    private fun String.isUpiLike(): Boolean {
        val lower = lowercase(Locale.ROOT)
        if ("upi" in lower) return true
        if ("vpa" in lower) return true
        if ("bhim" in lower) return true
        if ("imps" in lower) return true
        if ("neft" in lower) return true
        if ("@ok" in lower || "@ybl" in lower || "@paytm" in lower || "@upi" in lower || "@axl" in lower) return true
        if (referencePattern.containsMatchIn(this)) return true
        if (Regex("\\brrn\\b", RegexOption.IGNORE_CASE).containsMatchIn(this)) return true
        // Bank account txns that look transactional
        if (Regex("\\ba/?c\\b", RegexOption.IGNORE_CASE).containsMatchIn(this) &&
            (debitWords.any { it in lower } || creditWords.any { it in lower })
        ) return true
        return false
    }

    private fun String.extractDirection(): TransactionDirection? {
        val lower = lowercase(Locale.ROOT)
        return when {
            debitWords.any { it in lower } -> TransactionDirection.DEBIT
            creditWords.any { it in lower } -> TransactionDirection.CREDIT
            else -> null
        }
    }

    private fun String.extractAmountPaise(): Long? {
        val amountText = amountPatterns.firstNotNullOfOrNull { regex ->
            regex.find(this)?.groupValues?.get(1)
        } ?: return null

        return amountText
            .replace(",", "")
            .toBigDecimalOrNull()
            ?.setScale(2, RoundingMode.HALF_UP)
            ?.multiply(BigDecimal(100))
            ?.longValueExact()
    }

    private fun String.extractOccurredOn(): LocalDate? {
        val patterns = listOf(datePattern, bareDatePattern)
        for (pattern in patterns) {
            val match = pattern.find(this) ?: continue
            val day = match.groupValues[1].toIntOrNull() ?: continue
            val month = match.groupValues[2].toMonthOrNull() ?: continue
            val year = match.groupValues[3].toYearOrNull() ?: continue
            val parsed = runCatching { LocalDate.of(year, month, day) }.getOrNull()
            if (parsed != null) return parsed
        }
        return null
    }

    private fun String.extractReferenceNumber(): String? =
        referencePattern.find(this)
            ?.groupValues
            ?.get(1)
            ?.trim()
            ?.trimEnd('.', ',', ';')

    private fun String.extractAccountSuffix(): String? =
        accountPattern.find(this)
            ?.groupValues
            ?.get(1)
            ?.filter(Char::isDigit)
            ?.takeIf { it.isNotBlank() }

    private fun String.extractCounterparty(direction: TransactionDirection): String? {
        val patterns = when (direction) {
            TransactionDirection.DEBIT -> debitCounterpartyPatterns
            TransactionDirection.CREDIT -> creditCounterpartyPatterns
        }

        return patterns.firstNotNullOfOrNull { pattern ->
            pattern.find(this)?.groupValues?.get(1)?.cleanCounterparty()
        }
    }

    private fun String.cleanCounterparty(): String? =
        trim()
            .trim('-', ':', ',', '.', ';')
            .replace(Regex("\\s+"), " ")
            .takeIf { it.isNotBlank() }

    private fun Long.toLocalDate(zoneId: ZoneId): LocalDate =
        Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

    private fun String.toMonthOrNull(): Month? {
        val text = lowercase(Locale.ROOT)
        text.toIntOrNull()?.let { number ->
            return runCatching { Month.of(number) }.getOrNull()
        }

        return monthAliases[text.take(3)]
    }

    private fun String.toYearOrNull(): Int? {
        val value = toIntOrNull() ?: return null
        return if (value < 100) 2000 + value else value
    }

    private companion object {
        val otpPattern = Regex("\\b(?:otp|one\\s*time\\s*password|verification\\s*code)\\b", RegexOption.IGNORE_CASE)
        val debitWords = listOf(
            "debited", "debit", "paid", "sent", "spent", "transferred", "withdrawn",
            "deducted", "purchase", "paying", "txn of", "txn:", "txn for",
        )
        val creditWords = listOf(
            "credited", "credit", "received", "deposited", "added to", "refund",
            "reversed", "refunded",
        )

        val amountPatterns = listOf(
            // "Rs.123 debited"
            Regex(
                "(?i)(?:rs\\.?|inr|₹)\\s*([0-9][0-9,]*(?:\\.\\d{1,2})?)\\s+(?:has\\s+been\\s+|is\\s+|was\\s+)?(?:debited|credited|paid|received|sent|spent|transferred|withdrawn|deducted)"
            ),
            // "debited by Rs.123 / for INR 123 / with 123"
            Regex(
                "(?i)\\b(?:debited|credited|paid|received|sent|spent|transferred|withdrawn|deducted|refunded)\\s+(?:by|with|for|of)?\\s*(?:rs\\.?|inr|₹)?\\s*([0-9][0-9,]*(?:\\.\\d{1,2})?)"
            ),
            // "payment of Rs.123 / amount Rs.123 / txn of Rs.123"
            Regex("(?i)\\b(?:payment|amount|txn|transaction|sum)\\s+(?:of\\s+)?(?:rs\\.?|inr|₹)\\s*([0-9][0-9,]*(?:\\.\\d{1,2})?)"),
            // Generic "Rs.123" fallback
            Regex("(?i)(?:rs\\.?|inr|₹)\\s*([0-9][0-9,]*(?:\\.\\d{1,2})?)")
        )

        val datePattern = Regex(
            "(?i)\\b(?:on\\s+date|on|dated|dt|date)\\s*([0-3]?\\d)[\\s/-]*([a-z]{3,9}|\\d{1,2})[\\s/-]*([0-9]{2,4})\\b"
        )
        val bareDatePattern = Regex(
            "\\b([0-3]?\\d)[/-]([0-1]?\\d|[A-Za-z]{3,9})[/-]([0-9]{2,4})\\b"
        )
        val referencePattern = Regex(
            "(?i)\\b(?:upi\\s*)?(?:ref(?:erence)?\\s*(?:no\\.?|num|number|id)?|refno|rrn|utr|txn(?:\\s*(?:id|no))?|transaction\\s*(?:id|no))[:\\-\\s#]*([a-z0-9]{6,})\\b"
        )
        val accountPattern = Regex("(?i)\\bA/?[Cc]\\.?\\s*(?:no\\.?\\s*)?[xX*]*(\\d{3,6})\\b")

        private const val STOP = "(?=\\s+(?:Refno|Ref\\b|Ref\\.|Ref:|UPI|RRN|UTR|If\\s+not|on\\s+date|on\\s+\\d|dated|via|through|using|info|info:|Avl|Available|Bal|Balance|-[A-Z]{2,5}\\b|\\.|,)|$)"

        val debitCounterpartyPatterns = listOf(
            Regex("(?i)\\b(?:trf|transfer(?:red)?)\\s+to\\s+(.+?)$STOP"),
            Regex("(?i)\\b(?:paid|sent|payment)\\s+to\\s+(.+?)$STOP"),
            Regex("(?i)\\bto\\s+VPA\\s+(.+?)$STOP"),
            Regex("(?i)\\bto\\s+(.+?)\\s+(?:on|via|using|through)\\s+UPI"),
            Regex("(?i)\\btowards\\s+(.+?)$STOP")
        )
        val creditCounterpartyPatterns = listOf(
            Regex("(?i)\\bfrom\\s+VPA\\s+(.+?)$STOP"),
            Regex("(?i)\\bfrom\\s+(.+?)$STOP"),
            Regex("(?i)\\bby\\s+(.+?)$STOP"),
            Regex("(?i)\\bcredited\\s+(?:by|with)\\s+\\S+\\s+from\\s+(.+?)$STOP")
        )

        val monthAliases = mapOf(
            "jan" to Month.JANUARY,
            "feb" to Month.FEBRUARY,
            "mar" to Month.MARCH,
            "apr" to Month.APRIL,
            "may" to Month.MAY,
            "jun" to Month.JUNE,
            "jul" to Month.JULY,
            "aug" to Month.AUGUST,
            "sep" to Month.SEPTEMBER,
            "oct" to Month.OCTOBER,
            "nov" to Month.NOVEMBER,
            "dec" to Month.DECEMBER
        )
    }
}
