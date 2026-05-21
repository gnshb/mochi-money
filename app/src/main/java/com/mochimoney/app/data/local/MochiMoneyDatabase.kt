package com.mochimoney.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        UpiTransactionEntity::class,
        CategoryEntity::class,
        CategoryRuleEntity::class,
    ],
    version = 2,
    exportSchema = false
)
abstract class MochiMoneyDatabase : RoomDatabase() {
    abstract fun upiTransactionDao(): UpiTransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun categoryRuleDao(): CategoryRuleDao
}
