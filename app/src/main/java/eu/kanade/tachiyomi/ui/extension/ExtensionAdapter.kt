package eu.kanade.tachiyomi.ui.extension

import android.widget.TextView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.extension.ExtensionAdapter.OnButtonClickListener
import uy.kohesive.injekt.injectLazy
import Komari.domain.base.BasePreferences
import Komari.domain.base.BasePreferences.ExtensionInstaller

/**
 * Adapter that holds the catalogue cards.
 *
 * @param listener instance of [OnButtonClickListener].
 */
class ExtensionAdapter(val listener: OnButtonClickListener) :
    FlexibleAdapter<IFlexible<*>>(null, listener, true) {

    val basePreferences: BasePreferences by injectLazy()
    val preferences: PreferencesHelper by injectLazy()

    var installedSortOrder = preferences.installedExtensionsOrder().get()
    var installPrivately = basePreferences.extensionInstaller().get() == ExtensionInstaller.PRIVATE

    init {
        setDisplayHeadersAtStartUp(true)
    }

    /**
     * Listener for browse item clicks.
     */
    val buttonClickListener: OnButtonClickListener = listener

    interface OnButtonClickListener {
        fun onButtonClick(position: Int)
        fun onCancelClick(position: Int)
        fun onUpdateAllClicked(position: Int)
        fun onExtSortClicked(view: TextView, position: Int)
    }
}
