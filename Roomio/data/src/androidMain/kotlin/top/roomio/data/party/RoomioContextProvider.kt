package top.roomio.data.party

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

/**
 * Initializes [AppContextHolder] from the application context. A ContentProvider
 * is created automatically before any other app component, so the LiveKit voice
 * client always has an application context available without launcher wiring.
 */
internal class RoomioContextProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        AppContextHolder.appContext = context?.applicationContext ?: return false
        return true
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
