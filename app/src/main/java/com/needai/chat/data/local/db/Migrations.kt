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

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE sessions ADD COLUMN type TEXT NOT NULL DEFAULT 'single'")
            db.execSQL("ALTER TABLE sessions ADD COLUMN skillIds TEXT DEFAULT NULL")
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE skills ADD COLUMN voiceId TEXT NOT NULL DEFAULT ''")
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE skills ADD COLUMN avatarPath TEXT NOT NULL DEFAULT ''")
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE skills ADD COLUMN enableMemory INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE sessions ADD COLUMN summaryText TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE sessions ADD COLUMN summaryEndMessageId INTEGER DEFAULT NULL")
            db.execSQL("ALTER TABLE model_configs ADD COLUMN contextWindow INTEGER NOT NULL DEFAULT 8192")
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE messages ADD COLUMN isRead INTEGER NOT NULL DEFAULT 1")
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `notification_templates` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `label` TEXT NOT NULL,
                    `prompt` TEXT NOT NULL,
                    `isBuiltin` INTEGER NOT NULL DEFAULT 0,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

}
