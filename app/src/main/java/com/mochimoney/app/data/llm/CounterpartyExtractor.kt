package com.mochimoney.app.data.llm

import com.mochimoney.app.domain.model.TransactionDirection

/**
 * Runs an on-device LLM to pull the counterparty (merchant or person) out of a transaction SMS.
 * Implementations wrap a specific backend (MediaPipe model file, or Gemini Nano via AICore).
 */
interface CounterpartyExtractor {
    /**
     * Returns the cleaned counterparty name, or null if the model can't determine one.
     * [currentGuess] is the regex parser's best guess, given to the model as a hint.
     */
    suspend fun extract(
        smsBody: String,
        direction: TransactionDirection,
        currentGuess: String?,
    ): String?

    /** Release native resources. Safe to call repeatedly. */
    fun close()
}

/**
 * Builds a tight, single-turn prompt. Small models do best with explicit rules, worked examples,
 * the raw SMS, and a demand for bare output — so we few-shot it and then post-process the answer.
 */
internal object CounterpartyPrompt {
    fun build(smsBody: String, direction: TransactionDirection, currentGuess: String?): String {
        val flow = if (direction == TransactionDirection.CREDIT) {
            "money was RECEIVED (a credit), so the counterparty is the SENDER/source"
        } else {
            "money was SENT (a debit), so the counterparty is the PAYEE/merchant"
        }
        return buildString {
            append("You extract the counterparty from an Indian bank/UPI transaction SMS — the OTHER ")
            append("party in the payment: the merchant, business, app, or person.\n\n")
            append("Rules:\n")
            append("- The bank that SENT the SMS (SBI, HDFC, ICICI, Axis, Kotak, PNB, BoB, etc.) is the ")
            append("user's OWN bank. It is NEVER the counterparty — ignore it.\n")
            append("- NEVER answer with payment-rail or generic words: UPI, IMPS, NEFT, RTGS, BHIM, ")
            append("VPA, A/c, Ref, payment, transaction, debit, credit, transfer, bank.\n")
            append("- Drop filler like \"customer\", \"user\", \"Dear\", account numbers, ref numbers, and URLs.\n")
            append("- For a refund, the counterparty is the merchant issuing the refund.\n")
            append("- The counterparty is a real merchant/brand/app/shop or a person's name.\n")
            append("- If there is genuinely no identifiable counterparty, reply exactly NONE.\n")
            append("- Reply with ONLY the name in Title Case. No labels, quotes, punctuation, or explanation.\n\n")
            append("Examples:\n")
            append("SMS: \"Dear Swiggy customer, your refund reference number for Rs 8 is 262603702041-1-R1. https://r.swiggy.com/refunds\"\n")
            append("Answer: Swiggy\n")
            append("SMS: \"Dear SBI User, your A/c X5982-credited by Rs.36.00 on 19May26 transfer from Instamart Ref No 872117861396 -SBI\"\n")
            append("Answer: Instamart\n")
            append("SMS: \"Rs.250 debited from HDFC Bank A/c xx12 to ZOMATO via UPI. Ref 4456.\"\n")
            append("Answer: Zomato\n")
            append("SMS: \"INR 1500 credited to your ICICI account from RAHUL SHARMA UPI/123.\"\n")
            append("Answer: Rahul Sharma\n")
            append("SMS: \"Rs 99 paid to BLINKIT via UPI from Axis Bank a/c **123. UPI Ref 5567.\"\n")
            append("Answer: Blinkit\n")
            append("SMS: \"Your A/c XX12 debited Rs.40 by UPI Ref 998877. -Canara Bank\"\n")
            append("Answer: NONE\n\n")
            append("Now this SMS — $flow.\n")
            append("SMS: \"")
            append(smsBody.trim().replace("\n", " "))
            append("\"\nAnswer:")
        }
    }

    private val bankNoise = setOf(
        "sbi", "hdfc", "icici", "axis", "kotak", "pnb", "bob", "boi", "canara", "union",
        "yes bank", "idfc", "indusind", "rbl", "federal", "bank", "customer", "user", "none",
        // Payment rails / generic words the model must never emit as a counterparty.
        "upi", "imps", "neft", "rtgs", "bhim", "vpa", "a/c", "ac", "ref", "payment",
        "transaction", "txn", "debit", "credit", "transfer", "account", "sms",
    )

    /** Normalizes raw model output into a usable, Title-Cased counterparty, or null. */
    fun clean(raw: String?): String? {
        if (raw == null) return null
        var text = raw.substringBefore("<|im_end|>").substringBefore("<|im_start|>").trim()
        text = text.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() } ?: return null
        text = text.substringAfter("Answer:", text).trim()
        text = text.trim('"', '\'', '`', '.', ',', ':', ';', ' ')
        text = text.removePrefix("Counterparty").trimStart(':', '-', ' ')
        // Strip trailing filler the model sometimes appends ("Swiggy customer" -> "Swiggy").
        text = text.replace(Regex("\\b(customer|user|account|a/c)\\b\\s*$", RegexOption.IGNORE_CASE), "").trim()
        if (text.isBlank()) return null
        if (text.length > 60) return null
        if (text.lowercase() in bankNoise) return null
        return titleCaseCounterparty(text)
    }
}

/**
 * Title-cases a counterparty for display: first letter of each word upper, rest lower.
 * Leaves all-caps acronyms of length <= 3 (e.g. "PVR") and known stylings reasonable enough.
 */
fun titleCaseCounterparty(raw: String): String {
    val trimmed = raw.trim().replace(Regex("\\s+"), " ")
    if (trimmed.isEmpty()) return trimmed
    return trimmed.split(" ").joinToString(" ") { word ->
        when {
            word.isEmpty() -> word
            word.length <= 3 && word.all { it.isLetter() && it.isUpperCase() } -> word
            else -> word.lowercase().replaceFirstChar { it.uppercase() }
        }
    }
}
