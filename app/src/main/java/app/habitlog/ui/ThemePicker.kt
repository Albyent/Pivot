package app.habitlog.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.habitlog.model.AppAccent

@Composable
internal fun ThemePicker(current: AppAccent, onSelect: (AppAccent) -> Unit, onClose: () -> Unit) {
    PickerDialog("App theme", onClose) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth().heightIn(max = 355.dp).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(AppAccent.entries, key = AppAccent::name) { option ->
                val selected = option == current
                val tint = colorResource(option.colorRes)
                Surface(
                    Modifier.selectable(selected, role = Role.RadioButton, onClick = { onSelect(option) }),
                    shape = RoundedCornerShape(13.dp),
                    color = if (selected) lerp(Ink.recessed, tint, 0.12f) else Ink.recessed,
                    border = BorderStroke(1.dp, if (selected) tint else Ink.border),
                ) {
                    Column(
                        Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(0.38f, 0.68f, 1f).forEach { intensity ->
                                Box(Modifier.size(14.dp).background(lerp(Ink.recessed, tint, intensity), RoundedCornerShape(3.dp)))
                            }
                            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                                if (selected) Icon(Icons.Outlined.Check, null, Modifier.size(16.dp), tint = tint)
                            }
                        }
                        Text(option.label, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}
