package eu.kanade.tachiyomi.ui.source

import android.content.res.ColorStateList
import android.view.View
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.R
import komari.i18n.MR
import komari.util.lang.getString
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.databinding.SourceItemBinding
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.icon
import eu.kanade.tachiyomi.source.includeLangInName
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.view.compatToolTipText
import android.R as AR

class SourceHolder(view: View, val adapter: SourceAdapter) :
    BaseFlexibleViewHolder(view, adapter) {

    val binding = SourceItemBinding.bind(view)

    init {
        binding.sourcePin.setOnClickListener {
            adapter.sourceListener.onPinClick(flexibleAdapterPosition)
        }
        binding.sourceLatest.setOnClickListener {
            adapter.sourceListener.onLatestClick(flexibleAdapterPosition)
        }
    }

    fun bind(item: SourceItem) {
        val source = item.source
        // setCardEdges(item)

        val underPinnedSection = item.header?.code?.equals(SourcePresenter.PINNED_KEY) ?: false
        val underLastUsedSection = item.header?.code?.equals(SourcePresenter.LAST_USED_KEY) ?: false
        val isPinned = item.isPinned ?: underPinnedSection
        val showLanguage = source.includeLangInName(adapter.enabledLanguages, adapter.extensionManager)
        val sourceName = if (showLanguage && (underPinnedSection || underLastUsedSection)) source.toString() else source.name
        binding.title.text = sourceName

        binding.sourcePin.apply {
            imageTintList = ColorStateList.valueOf(
                context.getResourceColor(
                    if (isPinned) {
                        R.attr.colorSecondary
                    } else {
                        AR.attr.textColorSecondary
                    },
                ),
            )
            compatToolTipText = context.getString(if (isPinned) MR.strings.unpin else MR.strings.pin)
            setImageResource(
                if (isPinned) {
                    R.drawable.ic_pin_24dp
                } else {
                    R.drawable.ic_pin_outline_24dp
                },
            )
        }

        // Set circle letter image.
        itemView.post {
            val icon = source.icon()
            when {
                icon != null -> binding.sourceImage.setImageDrawable(icon)
                item.source.id == LocalSource.ID -> binding.sourceImage.setImageResource(R.mipmap.ic_local_source)
            }
        }

        binding.sourceLatest.isVisible = source.supportsLatest
    }

    override fun getFrontView(): View {
        return binding.card
    }

    override fun getRearStartView(): View {
        return binding.startView
    }

    override fun getRearEndView(): View {
        return binding.endView
    }
}
