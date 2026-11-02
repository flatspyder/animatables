package com.singleoriginapps.animatables.catalog.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.singleoriginapps.animatables.AnimatablesMotion
import com.singleoriginapps.animatables.catalog.CatalogTheme
import com.singleoriginapps.animatables.catalog.model.demos

@Composable
fun AnimatablesCatalog(initialEffect: String? = null) {
    val systemDark = isSystemInDarkTheme()
    var dark by rememberSaveable { mutableStateOf(systemDark) }
    var motion by rememberSaveable { mutableStateOf(true) }
    var inspected by rememberSaveable { mutableStateOf(initialEffect) }
    LaunchedEffect(initialEffect) {
        if (initialEffect != null) inspected = initialEffect
    }
    CatalogTheme(dark) {
        AnimatablesMotion(enabled = motion) {
            val selected = demos.find { it.id == inspected }
            if (selected != null) {
                BackHandler { inspected = null }
                EffectInspectorScreen(
                    selected,
                    motion,
                    { motion = it },
                    dark,
                    { dark = it },
                    onClose = { inspected = null }
                )
            } else {
                CatalogHomeScreen(
                    dark,
                    { dark = it },
                    motion,
                    { motion = it },
                    onInspect = { inspected = it }
                )
            }
        }
    }
}
