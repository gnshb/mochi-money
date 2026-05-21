package com.mochimoney.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CategoryRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(rule: CategoryRuleEntity)

    @Query("SELECT * FROM category_rules WHERE counterpartyKey = :counterpartyKey LIMIT 1")
    fun findByCounterpartyKey(counterpartyKey: String): CategoryRuleEntity?
}
