package com.personal.journal.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.personal.journal.data.TAG_COLOUR_SLOTS

@Database(entities = [EntryEntity::class, TagEntity::class, EntryTagCrossRef::class], version = 2, exportSchema = true)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun tagDao(): TagDao

    companion object {
        fun create(context: Context): JournalDatabase =
            Room.databaseBuilder(context, JournalDatabase::class.java, "journal.db").addMigrations(MIGRATION_1_2).build()
    }
}

/** Adds tags.colourSlot and spreads existing tags over the palette in name order (names are unique). */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tags ADD COLUMN colourSlot INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE tags SET colourSlot = (SELECT COUNT(*) FROM tags t2 WHERE t2.name < tags.name) % $TAG_COLOUR_SLOTS")
    }
}
