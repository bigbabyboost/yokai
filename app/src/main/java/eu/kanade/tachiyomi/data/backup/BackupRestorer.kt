package eu.kanade.tachiyomi.data.backup

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupHistory
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSerializer
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import eu.kanade.tachiyomi.data.backup.models.BooleanPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.FloatPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.LongPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringSetPreferenceValue
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.library.CustomMangaManager
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.preference.AndroidPreferenceStore
import eu.kanade.tachiyomi.data.preference.PreferenceStore
import eu.kanade.tachiyomi.source.sourcePreferences
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import okio.buffer
import okio.gzip
import okio.source
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date

class BackupRestorer(context: Context, notifier: BackupNotifier) : AbstractBackupRestore<BackupManager>(context, notifier) {

    private val preferenceStore: PreferenceStore = Injekt.get()

    @SuppressLint("Recycle")
    override suspend fun performRestore(uri: Uri): Boolean {
        backupManager = BackupManager(context)

        val stream = context.contentResolver.openInputStream(uri)
        val backupString = stream!!.source().gzip().buffer().use { it.readByteArray() }
        val backup = backupManager.parser.decodeFromByteArray(BackupSerializer, backupString)

        restoreAmount = backup.backupManga.size + 1 // +1 for categories

        // Restore categories
        if (backup.backupCategories.isNotEmpty()) {
            restoreCategories(backup.backupCategories)
        }

        // Store source mapping for error messages
        val backupMaps = backup.backupBrokenSources.map { BackupSource(it.name, it.sourceId) } + backup.backupSources
        sourceMapping = backupMaps.associate { it.sourceId to it.name }

        return coroutineScope {
            restoreAppPreferences(backup.backupPreferences)
            restoreSourcePreferences(backup.backupSourcePreferences)

            // Restore individual manga
            backup.backupManga.forEach {
                if (!isActive) {
                    return@coroutineScope false
                }

                restoreManga(it, backup.backupCategories)
            }
            true
        }
        // TODO: optionally trigger online library + tracker update
    }

    private fun restoreCategories(backupCategories: List<BackupCategory>) {
        db.inTransaction {
            backupManager.restoreCategories(backupCategories)
        }

        restoreProgress += 1
        showRestoreProgress(restoreProgress, restoreAmount, context.getString(R.string.categories))
    }

    private fun restoreManga(backupManga: BackupManga, backupCategories: List<BackupCategory>) {
        val manga = backupManga.getMangaImpl()
        val chapters = backupManga.getChaptersImpl()
        val categories = backupManga.categories
        val history =
            backupManga.brokenHistory.map { BackupHistory(it.url, it.lastRead, it.readDuration) } + backupManga.history
        val tracks = backupManga.getTrackingImpl()
        val customManga = backupManga.getCustomMangaInfo()

        try {
            val dbManga = backupManager.getMangaFromDatabase(manga)
            if (dbManga == null) {
                // Manga not in database
                restoreExistingManga(manga, chapters, categories, history, tracks, backupCategories, customManga)
            } else {
                // Manga in database
                // Copy information from manga already in database
                backupManager.restoreExistingManga(manga, dbManga)
                // Fetch rest of manga information
                restoreNewManga(manga, chapters, categories, history, tracks, backupCategories, customManga)
            }
        } catch (e: Exception) {
            val sourceName = sourceMapping[manga.source] ?: manga.source.toString()
            errors.add(Date() to "${manga.title} [$sourceName]: ${e.message}")
        }

        restoreProgress += 1
        showRestoreProgress(restoreProgress, restoreAmount, manga.title)
        LibraryUpdateJob.updateMutableFlow.tryEmit(manga.id)
    }

    /**
     * Fetches manga information
     *
     * @param manga manga that needs updating
     * @param chapters chapters of manga that needs updating
     * @param categories categories that need updating
     */
    private fun restoreExistingManga(
        manga: Manga,
        chapters: List<Chapter>,
        categories: List<Int>,
        history: List<BackupHistory>,
        tracks: List<Track>,
        backupCategories: List<BackupCategory>,
        customManga: CustomMangaManager.MangaJson?,
    ) {
        val fetchedManga = backupManager.restoreNewManga(manga)
        fetchedManga.id ?: return

        backupManager.restoreChapters(fetchedManga, chapters)
        restoreExtras(fetchedManga, categories, history, tracks, backupCategories, customManga)
    }

    private fun restoreNewManga(
        backupManga: Manga,
        chapters: List<Chapter>,
        categories: List<Int>,
        history: List<BackupHistory>,
        tracks: List<Track>,
        backupCategories: List<BackupCategory>,
        customManga: CustomMangaManager.MangaJson?,
    ) {
        backupManager.restoreChapters(backupManga, chapters)
        restoreExtras(backupManga, categories, history, tracks, backupCategories, customManga)
    }

    private fun restoreExtras(
        manga: Manga,
        categories: List<Int>,
        history: List<BackupHistory>,
        tracks: List<Track>,
        backupCategories: List<BackupCategory>,
        customManga: CustomMangaManager.MangaJson?,
    ) {
        backupManager.restoreCategories(manga, categories, backupCategories)
        backupManager.restoreHistoryForManga(history)
        backupManager.restoreTrackForManga(manga, tracks)
        customManga?.id = manga.id!!
        customManga?.let { customMangaManager.saveMangaInfo(it) }
    }

    private fun restoreAppPreferences(preferences: List<BackupPreference>) {
        restorePreferences(preferences, preferenceStore)
    }

    private fun restoreSourcePreferences(preferences: List<BackupSourcePreferences>) {
        preferences.forEach {
            val sourcePrefs = AndroidPreferenceStore(context, sourcePreferences(it.sourceKey))
            restorePreferences(it.prefs, sourcePrefs)
        }
    }

    private fun restorePreferences(
        toRestore: List<BackupPreference>,
        preferenceStore: PreferenceStore,
    ) {
        val prefs = preferenceStore.getAll()
        toRestore.forEach { (key, value) ->
            when (value) {
                is IntPreferenceValue -> {
                    if (prefs[key] is Int?) {
                        preferenceStore.getInt(key).set(value.value)
                    }
                }
                is LongPreferenceValue -> {
                    if (prefs[key] is Long?) {
                        preferenceStore.getLong(key).set(value.value)
                    }
                }
                is FloatPreferenceValue -> {
                    if (prefs[key] is Float?) {
                        preferenceStore.getFloat(key).set(value.value)
                    }
                }
                is StringPreferenceValue -> {
                    if (prefs[key] is String?) {
                        preferenceStore.getString(key).set(value.value)
                    }
                }
                is BooleanPreferenceValue -> {
                    if (prefs[key] is Boolean?) {
                        preferenceStore.getBoolean(key).set(value.value)
                    }
                }
                is StringSetPreferenceValue -> {
                    if (prefs[key] is Set<*>?) {
                        preferenceStore.getStringSet(key).set(value.value)
                    }
                }
            }
        }
    }
}
