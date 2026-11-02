package com.singleoriginapps.animatables.catalog.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.singleoriginapps.animatables.catalog.CatalogTheme

@Composable
internal fun ProfileSkeleton() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .width(148.dp)
                    .height(16.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
            Box(
                Modifier
                    .width(124.dp)
                    .height(12.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
        }
    }
}

@Preview(name = "Profile Skeleton · Light", widthDp = 300, showBackground = true)
@Composable
private fun ProfileSkeletonLightPreview() {
    CatalogTheme {
        Surface {
            Box(Modifier.padding(16.dp)) {
                ProfileSkeleton()
            }
        }
    }
}

@Preview(name = "Profile Skeleton · Dark", widthDp = 300, showBackground = true)
@Composable
private fun ProfileSkeletonDarkPreview() {
    CatalogTheme(dark = true) {
        Surface {
            Box(Modifier.padding(16.dp)) {
                ProfileSkeleton()
            }
        }
    }
}
