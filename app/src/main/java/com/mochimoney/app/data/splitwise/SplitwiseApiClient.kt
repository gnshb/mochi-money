package com.mochimoney.app.data.splitwise

import com.mochimoney.app.domain.model.TransactionDirection
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.OffsetDateTime

class SplitwiseApiClient {
    fun getCurrentUser(apiKey: String): SplitwiseUser {
        val root = getJson(apiKey, "get_current_user")
        val user = root.getJSONObject("user")
        return SplitwiseUser(
            id = user.getLong("id"),
            name = listOf(user.optString("first_name"), user.optString("last_name"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString(" ")
                .ifBlank { user.optString("email", "Splitwise user") },
        )
    }

    fun getGroups(apiKey: String): List<SplitwiseGroup> {
        val root = getJson(apiKey, "get_groups")
        val groups = root.getJSONArray("groups")
        return List(groups.length()) { index ->
            val group = groups.getJSONObject(index)
            val id = group.getLong("id")
            SplitwiseGroup(
                id = id,
                name = group.optString("name", "Splitwise group $id"),
            )
        }
    }

    fun getExpenseShares(
        apiKey: String,
        currentUserId: Long,
        group: SplitwiseGroup,
    ): List<SplitwiseExpenseShare> {
        val expenses = mutableListOf<SplitwiseExpenseShare>()
        var offset = 0
        val limit = 100

        while (offset < limit * MaxPages) {
            val root = getJson(
                apiKey = apiKey,
                path = "get_expenses",
                query = mapOf(
                    "group_id" to group.id.toString(),
                    "limit" to limit.toString(),
                    "offset" to offset.toString(),
                ),
            )
            val page = root.getJSONArray("expenses")
            for (index in 0 until page.length()) {
                page.getJSONObject(index)
                    .toShare(currentUserId = currentUserId, group = group)
                    ?.let(expenses::add)
            }
            if (page.length() < limit) break
            offset += limit
        }

        return expenses
    }

    private fun JSONObject.toShare(
        currentUserId: Long,
        group: SplitwiseGroup,
    ): SplitwiseExpenseShare? {
        if (!isNull("deleted_at") || optBoolean("payment", false)) return null

        val users = optJSONArray("users") ?: return null
        val currentUser = (0 until users.length())
            .map { users.getJSONObject(it) }
            .firstOrNull { userExpense ->
                val nestedUserId = userExpense.optJSONObject("user")?.optLong("id", 0L) ?: 0L
                val directUserId = userExpense.optLong("user_id", 0L)
                nestedUserId == currentUserId || directUserId == currentUserId
            }
            ?: return null

        val paidPaise = currentUser.optString("paid_share").toPaise()
        val owedPaise = currentUser.optString("owed_share").toPaise()
        val netPaise = paidPaise - owedPaise
        if (netPaise == 0L) return null

        return SplitwiseExpenseShare(
            expenseId = getLong("id"),
            groupId = group.id,
            groupName = group.name,
            description = optString("description", "Splitwise expense").ifBlank { "Splitwise expense" },
            date = OffsetDateTime.parse(getString("date")).toLocalDate(),
            currency = optString("currency_code", "INR").ifBlank { "INR" },
            direction = if (netPaise > 0L) TransactionDirection.CREDIT else TransactionDirection.DEBIT,
            amountPaise = kotlin.math.abs(netPaise),
        )
    }

    private fun getJson(
        apiKey: String,
        path: String,
        query: Map<String, String> = emptyMap(),
    ): JSONObject {
        val queryString = query.entries
            .joinToString("&") { (key, value) ->
                "${key.urlEncode()}=${value.urlEncode()}"
            }
            .takeIf { it.isNotBlank() }
            ?.let { "?$it" }
            .orEmpty()
        val connection = URL("$BaseUrl/$path$queryString").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = TimeoutMillis
        connection.readTimeout = TimeoutMillis
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")

        val status = connection.responseCode
        val body = runCatching {
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        }.getOrDefault("")

        if (status !in 200..299) {
            val message = runCatching {
                JSONObject(body.ifBlank { "{}" }).optString("error")
            }.getOrDefault("")
                .ifBlank { "Splitwise request failed with HTTP $status." }
            throw SplitwiseApiException(message)
        }

        return JSONObject(body)
    }

    private fun String.toPaise(): Long =
        BigDecimal(this.ifBlank { "0" })
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()

    private fun String.urlEncode(): String =
        URLEncoder.encode(this, Charsets.UTF_8.name())

    private companion object {
        const val BaseUrl = "https://secure.splitwise.com/api/v3.0"
        const val TimeoutMillis = 15_000
        const val MaxPages = 10
    }
}

class SplitwiseApiException(message: String) : Exception(message)
