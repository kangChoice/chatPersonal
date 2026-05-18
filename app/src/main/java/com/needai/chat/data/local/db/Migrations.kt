package com.needai.chat.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room 数据库兼容性 Migration 集合。
 *
 * 数据库版本升级时：
 * 1. 在此文件添加 Migration 常量（如 MIGRATION_8_9）
 * 2. 在 [AppDatabase] 中提升 version
 * 3. 在 [DatabaseModule] 的 .addMigrations() 链上注册：Migrations.MIGRATION_8_9
 *
 * 禁止使用 fallbackToDestructiveMigration() 破坏用户数据。
 */
object Migrations {

    // 示例：在 skills 表新增字段
    // val MIGRATION_8_9 = object : Migration(8, 9) {
    //     override fun migrate(db: SupportSQLiteDatabase) {
    //         db.execSQL("ALTER TABLE skills ADD COLUMN newField TEXT DEFAULT NULL")
    //     }
    // }

}
