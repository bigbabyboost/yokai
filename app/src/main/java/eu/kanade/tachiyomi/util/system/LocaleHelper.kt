package eu.kanade.tachiyomi.util.system

import android.content.Context
import androidx.core.os.LocaleListCompat
import eu.kanade.tachiyomi.R
import komari.i18n.MR
import komari.util.lang.getString
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.ui.source.SourcePresenter
import java.util.Locale

/**
 * Utility class to change the application's language in runtime.
 */
object LocaleHelper {

    /**
     * Returns Display name of a string language code
     */
    fun getSourceDisplayName(lang: String?, context: Context): String {
        return when (lang) {
            "", "other" -> context.getString(MR.strings.other)
            SourcePresenter.LAST_USED_KEY -> context.getString(MR.strings.last_used)
            SourcePresenter.PINNED_KEY -> context.getString(MR.strings.pinned)
            "all" -> context.getString(MR.strings.all)
            else -> getDisplayName(lang)
        }
    }

    /**
     * Returns Display name of a string language code
     *
     * @param lang empty for system language
     */
    fun getDisplayName(lang: String?): String {
        if (lang == null) {
            return ""
        }

        val locale = if (lang.isEmpty()) {
            LocaleListCompat.getAdjustedDefault()[0]
        } else {
            getLocale(lang)
        } ?: Locale.getDefault()
        return locale.getDisplayName(locale).replaceFirstChar { it.uppercase(locale) }
    }

    /**
     * Return Locale from string language code
     */
    private fun getLocale(lang: String): Locale {
        val sp = lang.split("_", "-")
        return when (sp.size) {
            2 -> Locale(sp[0], sp[1])
            3 -> Locale(sp[0], sp[1], sp[2])
            else -> Locale(lang)
        }
    }
}
