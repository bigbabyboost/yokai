package eu.kanade.tachiyomi.data.database

import android.database.Cursor
import com.pushtorefresh.storio.sqlite.StorIOSQLite

inline fun StorIOSQLite.inTransaction(block: () -> Unit) {
    lowLevel().beginTransaction()
    try {
        block()
        lowLevel().setTransactionSuccessful()
    } finally {
        lowLevel().endTransaction()
    }
}

inline fun <T> StorIOSQLite.inTransactionReturn(block: () -> T): T {
    lowLevel().beginTransaction()
    try {
        val result = block()
        lowLevel().setTransactionSuccessful()
        return result
    } finally {
        lowLevel().endTransaction()
    }
}

fun Cursor.getBoolean(index: Int) = getLong(index) > 0
