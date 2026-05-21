package com.mochimoney.app.data

import android.content.Context

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

    private companion object {
        const val KEY_ONBOARDING_DONE = "has_completed_onboarding"
        const val KEY_MONTHLY_BUDGET_PAISE = "monthly_budget_paise"
        const val KEY_SENDER_PATTERN = "sender_pattern"
    }
}
