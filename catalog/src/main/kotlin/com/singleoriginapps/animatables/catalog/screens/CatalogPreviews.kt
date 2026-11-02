package com.singleoriginapps.animatables.catalog.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.singleoriginapps.animatables.AnimatablesMotion
import com.singleoriginapps.animatables.AnimatedCountBadge
import com.singleoriginapps.animatables.AnimatedStatusText
import com.singleoriginapps.animatables.ExpandableCard
import com.singleoriginapps.animatables.LoadingContent
import com.singleoriginapps.animatables.MotionContent
import com.singleoriginapps.animatables.SuccessCheck
import com.singleoriginapps.animatables.catalog.CatalogTheme
import com.singleoriginapps.animatables.catalog.components.ProfileSkeleton

@Preview(name = "Phone · Light", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun CatalogLightPreview() {
    CatalogTheme {
        AnimatablesMotion(false) {
            CatalogHomeScreen(false, {}, true, {})
        }
    }
}

@Preview(name = "Phone · Dark", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun CatalogDarkPreview() {
    CatalogTheme(true) {
        AnimatablesMotion(false) {
            CatalogHomeScreen(true, {}, true, {})
        }
    }
}

@Preview(name = "Tablet", widthDp = 1000, heightDp = 1000, showBackground = true)
@Composable
private fun CatalogTabletPreview() {
    CatalogTheme {
        AnimatablesMotion(false) {
            CatalogHomeScreen(false, {}, true, {})
        }
    }
}

@Preview(
    name = "Large text",
    widthDp = 412,
    heightDp = 915,
    fontScale = 1.5f,
    showBackground = true
)
@Composable
private fun CatalogLargeTextPreview() {
    CatalogTheme {
        AnimatablesMotion(false) {
            CatalogHomeScreen(false, {}, false, {})
        }
    }
}

@Preview(name = "RTL", widthDp = 412, heightDp = 915, locale = "ar", showBackground = true)
@Composable
private fun CatalogRtlPreview() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        CatalogTheme {
            AnimatablesMotion(false) {
                CatalogHomeScreen(false, {}, true, {})
            }
        }
    }
}

@Preview(name = "Component resting states", widthDp = 412, heightDp = 1000, showBackground = true)
@Composable
private fun RestingStatesPreview() {
    CatalogTheme {
        AnimatablesMotion(false) {
            Surface {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Text("Ready at rest.", style = MaterialTheme.typography.headlineLarge)
                    ExpandableCard(
                        true,
                        {},
                        "An expanded card"
                    ) {
                        Text("This content remains readable with animations disabled.")
                    }
                    AnimatedStatusText("All changes saved")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AnimatedCountBadge(0)
                        AnimatedCountBadge(3)
                        AnimatedCountBadge(123)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        SuccessCheck(true, Modifier.size(32.dp))
                        SuccessCheck(true, Modifier.size(64.dp))
                    }
                    LoadingContent(
                        false,
                        placeholder = { ProfileSkeleton() }
                    ) {
                        Text("Content has arrived")
                    }
                    Text(
                        "Structural loading placeholder",
                        style = MaterialTheme.typography.labelMedium
                    )
                    ProfileSkeleton()
                    MotionContent(true) { Text("Related content") }
                }
            }
        }
    }
}
