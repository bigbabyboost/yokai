package komari.presentation.widget.components

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import komari.i18n.MR
import komari.presentation.core.Constants
import komari.presentation.widget.ContainerModifier
import komari.presentation.widget.R
import komari.presentation.widget.util.stringResource

@Composable
fun LockedWidget() {
    val context = LocalContext.current
    val clazz = Class.forName(Constants.MAIN_ACTIVITY)
    val intent = Intent(context, clazz).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    Box(
        modifier = GlanceModifier
            .clickable(actionStartActivity(intent))
            .then(ContainerModifier)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(MR.strings.appwidget_unavailable_locked),
            style = TextStyle(
                color = ColorProvider(Color(context.getColor(R.color.appwidget_on_secondary_container))),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            ),
        )
    }
}
