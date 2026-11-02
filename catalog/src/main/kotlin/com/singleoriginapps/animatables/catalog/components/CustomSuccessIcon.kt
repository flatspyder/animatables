package com.singleoriginapps.animatables.catalog.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.singleoriginapps.animatables.catalog.CatalogTheme
import com.singleoriginapps.animatables.samples.MaterialSymbols

@Composable
internal fun CustomSuccessIcon() {
    Icon(MaterialSymbols.Verified, contentDescription = null, modifier = Modifier.fillMaxSize())
}

@Preview(name = "Custom Success Icon", widthDp = 100, heightDp = 100, showBackground = true)
@Composable
private fun CustomSuccessIconPreview() {
    CatalogTheme {
        Surface {
            Box(
                Modifier
                    .padding(24.dp)
                    .size(48.dp)
            ) {
                CustomSuccessIcon()
            }
        }
    }
}
