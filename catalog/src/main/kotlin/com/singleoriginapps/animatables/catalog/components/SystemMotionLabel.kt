package com.singleoriginapps.animatables.catalog.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.singleoriginapps.animatables.catalog.CatalogTheme
import com.singleoriginapps.animatables.catalog.rememberSystemAnimationScale

@Composable
internal fun SystemMotionLabel() {
    val scale = rememberSystemAnimationScale()
    Text(
        if (scale == 0f) "Android animations are disabled" else "Android animation scale: ${scale}×",
        style = MaterialTheme.typography.bodySmall,
        color = if (scale == 0f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
    )
}

@Preview(name = "System Motion Label", widthDp = 280, showBackground = true)
@Composable
private fun SystemMotionLabelPreview() {
    CatalogTheme {
        Surface {
            Box(Modifier.padding(16.dp)) {
                SystemMotionLabel()
            }
        }
    }
}
