package com.singleoriginapps.animatables.catalog.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.singleoriginapps.animatables.catalog.components.DemoCard
import com.singleoriginapps.animatables.catalog.components.OriginMark
import com.singleoriginapps.animatables.catalog.components.SystemMotionLabel
import com.singleoriginapps.animatables.catalog.model.Collection
import com.singleoriginapps.animatables.catalog.model.demos

@Composable
internal fun CatalogHomeScreen(
    dark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    motion: Boolean,
    onMotionChange: (Boolean) -> Unit,
    onInspect: (String) -> Unit = {}
) {
    var filter by rememberSaveable { mutableStateOf(Collection.All) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(320.dp),
            modifier = Modifier.safeDrawingPadding(),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OriginMark(Modifier.size(28.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "SINGLE ORIGIN",
                            style = MaterialTheme.typography.labelLarge,
                            letterSpacing = 2.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { onDarkChange(!dark) }) {
                            Text(if (dark) "Light theme" else "Dark theme")
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "THE COMPOSE MOTION LIBRARY",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.6.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Motion, made tangible.",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-1.5).sp
                        ),
                        modifier = Modifier.semantics { heading() }
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Play, interrupt and replay each transition.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (motion) "Library animations on" else "Library animations off",
                                style = MaterialTheme.typography.titleMedium
                            )
                            SystemMotionLabel()
                        }
                        Switch(
                            checked = motion,
                            onCheckedChange = onMotionChange,
                            modifier = Modifier.semantics { contentDescription = "Library motion" }
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "The collection",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.semantics { heading() }
                        )
                        Text(
                            "${demos.size} motion demos",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 10.dp)
                    ) {
                        items(Collection.entries.size) { i ->
                            val item = Collection.entries[i]
                            FilterChip(
                                selected = filter == item,
                                onClick = { filter = item },
                                label = { Text(item.title) }
                            )
                        }
                    }
                }
            }
            items(
                demos.filter { filter == Collection.All || it.group == filter },
                key = { it.id }
            ) { demo ->
                DemoCard(demo, onInspect)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(vertical = 16.dp)) {
                    Text("Motion with purpose.", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Theme tokens · Hoisted state · System animation scale\nPrototype 0.1 · singleoriginapps.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
