package com.suda.androidscrcpy.ui

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp

public val LocalIOSSwitchConfig: ProvidableCompositionLocal<IOSSwitchConfig> = compositionLocalOf {
    error("No IOSSwitchConfig provided")
}

public class IOSSwitchConfig(
    public val width: Dp,
    public val trackHeight: Dp,
    public val trackPadding: Dp,
) {
    internal val thumbSize by lazy { trackHeight - trackPadding * 2 }
}