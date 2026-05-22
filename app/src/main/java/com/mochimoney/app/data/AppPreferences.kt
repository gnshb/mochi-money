package com.mochimoney.app.data

import android.content.Context
import com.mochimoney.app.data.splitwise.SplitwiseGroup
import org.json.JSONArray
import org.json.JSONObject

class AppPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("mochi_money_preferences", Context.MODE_PRIVATE)

    var hasCompletedOnboarding: Boolean
        get() = preferences.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) {
            preferences.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()
        }

    var monthlyBudgetPaise: Long
        get() = preferences.getLong(KEY_MONTHLY_BUDGET_PAISE, 7_500_000L)
        set(value) {
            preferences.edit().putLong(KEY_MONTHLY_BUDGET_PAISE, value.coerceAtLeast(0L)).apply()
        }

    var senderPattern: String
        get() = preferences.getString(KEY_SENDER_PATTERN, "").orEmpty()
        set(value) {
            preferences.edit().putString(KEY_SENDER_PATTERN, value).apply()
        }

    var splitwiseEnabled: Boolean
        get() = preferences.getBoolean(KEY_SPLITWISE_ENABLED, false)
        set(value) {
            preferences.edit().putBoolean(KEY_SPLITWISE_ENABLED, value).apply()
        }

    var splitwiseApiKey: String
        get() = preferences.getString(KEY_SPLITWISE_API_KEY, "").orEmpty()
        set(value) {
            preferences.edit().putString(KEY_SPLITWISE_API_KEY, value.trim()).apply()
        }

    var splitwiseCurrentUserId: Long
        get() = preferences.getLong(KEY_SPLITWISE_CURRENT_USER_ID, 0L)
        set(value) {
            preferences.edit().putLong(KEY_SPLITWISE_CURRENT_USER_ID, value.coerceAtLeast(0L)).apply()
        }

    var splitwiseCurrentUserName: String
        get() = preferences.getString(KEY_SPLITWISE_CURRENT_USER_NAME, "").orEmpty()
        set(value) {
            preferences.edit().putString(KEY_SPLITWISE_CURRENT_USER_NAME, value.trim()).apply()
        }

    var splitwiseSelectedGroupIds: Set<Long>
        get() = preferences.getStringSet(KEY_SPLITWISE_SELECTED_GROUP_IDS, emptySet())
            .orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .toSet()
        set(value) {
            preferences.edit()
                .putStringSet(KEY_SPLITWISE_SELECTED_GROUP_IDS, value.map(Long::toString).toSet())
                .apply()
        }

    var splitwiseGroups: List<SplitwiseGroup>
        get() {
            val raw = preferences.getString(KEY_SPLITWISE_GROUPS, "[]").orEmpty()
            return runCatching {
                val array = JSONArray(raw)
                List(array.length()) { index ->
                    val item = array.getJSONObject(index)
                    SplitwiseGroup(
                        id = item.getLong("id"),
                        name = item.optString("name"),
                    )
                }
            }.getOrDefault(emptyList())
        }
        set(value) {
            val array = JSONArray()
            value.forEach { group ->
                array.put(
                    JSONObject()
                        .put("id", group.id)
                        .put("name", group.name),
                )
            }
            preferences.edit().putString(KEY_SPLITWISE_GROUPS, array.toString()).apply()
        }

    private companion object {
        const val KEY_ONBOARDING_DONE = "has_completed_onboarding"
        const val KEY_MONTHLY_BUDGET_PAISE = "monthly_budget_paise"
        const val KEY_SENDER_PATTERN = "sender_pattern"
        const val KEY_SPLITWISE_ENABLED = "splitwise_enabled"
        const val KEY_SPLITWISE_API_KEY = "splitwise_api_key"
        const val KEY_SPLITWISE_CURRENT_USER_ID = "splitwise_current_user_id"
        const val KEY_SPLITWISE_CURRENT_USER_NAME = "splitwise_current_user_name"
        const val KEY_SPLITWISE_SELECTED_GROUP_IDS = "splitwise_selected_group_ids"
        const val KEY_SPLITWISE_GROUPS = "splitwise_groups"
    }
}
