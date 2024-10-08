package eu.kanade.tachiyomi.ui.library.display

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.slider.Slider
import eu.kanade.tachiyomi.R
import komari.i18n.MR
import komari.util.lang.getString
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.databinding.LibraryDisplayLayoutBinding
import eu.kanade.tachiyomi.util.bindToPreference
import eu.kanade.tachiyomi.util.lang.addBetaTag
import eu.kanade.tachiyomi.util.lang.withSubtitle
import eu.kanade.tachiyomi.util.system.bottomCutoutInset
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.isLandscape
import eu.kanade.tachiyomi.util.system.topCutoutInset
import eu.kanade.tachiyomi.util.view.checkHeightThen
import eu.kanade.tachiyomi.util.view.numberOfRowsForValue
import eu.kanade.tachiyomi.util.view.rowsForValue
import eu.kanade.tachiyomi.widget.BaseLibraryDisplayView
import kotlin.math.roundToInt

class LibraryDisplayView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseLibraryDisplayView<LibraryDisplayLayoutBinding>(context, attrs) {

    var mainView: View? = null
    override fun inflateBinding() = LibraryDisplayLayoutBinding.bind(this)
    override fun initGeneralPreferences() {
        binding.displayGroup.bindToPreference(preferences.libraryLayout())
        binding.uniformGrid.bindToPreference(uiPreferences.uniformGrid()) {
            binding.staggeredGrid.isEnabled = !it
        }
        binding.outlineOnCovers.bindToPreference(uiPreferences.outlineOnCovers())
        binding.staggeredGrid.text = context.getString(MR.strings.use_staggered_grid).addBetaTag(context)
        binding.staggeredGrid.isEnabled = !uiPreferences.uniformGrid().get()
        binding.staggeredGrid.bindToPreference(preferences.useStaggeredGrid())
        binding.gridSeekbar.value = ((preferences.gridSize().get() + .5f) * 2f).roundToInt().toFloat()
        binding.resetGridSize.setOnClickListener {
            binding.gridSeekbar.value = 3f
        }

        binding.gridSeekbar.setLabelFormatter {
            val view = controller?.activity?.window?.decorView ?: mainView ?: this@LibraryDisplayView
            val mainText = (mainView ?: this@LibraryDisplayView).rowsForValue(it).toString()
            val mainOrientation = context.getString(
                if (context.isLandscape()) {
                    MR.strings.landscape
                } else {
                    MR.strings.portrait
                },
            )
            val alt = (
                if (view.measuredHeight >= 720.dpToPx) {
                    view.measuredHeight - 72.dpToPx
                } else {
                    view.measuredHeight
                }
                ) -
                (view.rootWindowInsets?.topCutoutInset() ?: 0) -
                (view.rootWindowInsets?.bottomCutoutInset() ?: 0)
            val altText = alt.numberOfRowsForValue(it).toString()
            val altOrientation = context.getString(
                if (context.isLandscape()) {
                    MR.strings.portrait
                } else {
                    MR.strings.landscape
                },
            )
            "$mainOrientation: $mainText • $altOrientation: $altText"
        }
        binding.gridSeekbar.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) {
                preferences.gridSize().set((value / 2f) - .5f)
            }
            setGridText(value)
        }
        binding.gridSeekbar.addOnSliderTouchListener(
            object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {}

                override fun onStopTrackingTouch(slider: Slider) {
                    preferences.gridSize().set((slider.value / 2f) - .5f)
                    setGridText(slider.value)
                }
            },
        )
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        checkHeightThen {
            setGridText(binding.gridSeekbar.value)
        }
    }

    private fun setGridText(progress: Float) {
        with(binding.gridSizeText) {
            val rows = (mainView ?: this@LibraryDisplayView).rowsForValue(progress)
            val titleText = context.getString(MR.strings.grid_size)
            val subtitleText = context.getString(MR.strings._per_row, rows)
            text = titleText.withSubtitle(context, subtitleText)
        }
    }
}
