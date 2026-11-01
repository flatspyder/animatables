package com.singleoriginapps.animatables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * A state-hoisted disclosure card with a full-width, keyboard-accessible header.
 *
 * Hoist [expanded] with `rememberSaveable` or a state holder. The header stays enabled while the
 * fast spatial/effects transition runs, so a second tap reverses it. Body reflow is intentional;
 * the outer surface is kept outside the clipped reveal. It inherits Material colors and shape.
 * Localize [expandedStateDescription] and [collapsedStateDescription] in the host app.
 */
@Composable
public fun ExpandableCard(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    expandedStateDescription: String = "Expanded",
    collapsedStateDescription: String = "Collapsed",
    content: @Composable ColumnScope.() -> Unit,
) {
    val enabled = rememberMotionEnabled()
    val spatial =
        if (enabled) MaterialTheme.motionScheme.fastSpatialSpec<IntSize>() else snap<IntSize>()
    val effects =
        if (enabled) MaterialTheme.motionScheme.fastEffectsSpec<Float>() else snap<Float>()
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = if (enabled) MaterialTheme.motionScheme.fastSpatialSpec() else snap(),
        label = "Disclosure chevron",
    )
    val chevronColor = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        stateDescription = if (expanded) expandedStateDescription
                        else collapsedStateDescription
                    }
                    .clickable(role = Role.Button) { onExpandedChange(!expanded) }
                    .sizeIn(minHeight = 64.dp)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium
                )
                Icon(
                    MaterialSymbols.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).graphicsLayer { rotationZ = rotation },
                    tint = chevronColor,
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(spatial, expandFrom = Alignment.Top) + fadeIn(effects),
                exit = shrinkVertically(spatial, shrinkTowards = Alignment.Top) + fadeOut(effects),
            ) {
                Column(
                    Modifier
                        .then(if (expanded) Modifier else Modifier.clearAndSetSemantics {})
                        .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    content = content,
                )
            }
        }
    }
}
