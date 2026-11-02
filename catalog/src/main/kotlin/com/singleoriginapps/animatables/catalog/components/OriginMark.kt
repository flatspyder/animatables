package com.singleoriginapps.animatables.catalog.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.singleoriginapps.animatables.catalog.CatalogTheme

@Composable
internal fun OriginMark(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        rotate(45f) {
            drawRoundRect(
                color,
                cornerRadius = CornerRadius(size.width * .28f)
            )
        }
        drawCircle(Color.White, size.minDimension * .12f, Offset(size.width / 2, size.height / 2))
    }
}

@Preview(name = "Origin Mark", widthDp = 200, heightDp = 100, showBackground = true)
@Composable
private fun OriginMarkPreview() {
    CatalogTheme {
        Surface {
            Row(
                Modifier.padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OriginMark(Modifier.size(24.dp))
                OriginMark(Modifier.size(36.dp))
                OriginMark(Modifier.size(48.dp))
            }
        }
    }
}
