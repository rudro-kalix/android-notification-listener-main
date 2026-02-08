package ee.kytt.androidnotificationlistener.ui.element

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class BarEntry(val x: Float, val yValues: FloatArray)

@Composable
fun Chart(
    modifier: Modifier = Modifier,
    xLabels: List<String>,
    entries: List<BarEntry>,
    entryColors: List<Color>
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        val labelCount = xLabels.size
        val entryCount = entries.size
        val colorCount = entryColors.size
        Text(
            text = "Chart placeholder ($labelCount labels, $entryCount entries, $colorCount colors)",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
