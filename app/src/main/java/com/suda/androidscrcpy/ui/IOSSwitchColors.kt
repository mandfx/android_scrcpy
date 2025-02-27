package com.suda.androidscrcpy.ui

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

public val LocalIOSSwitchColors: ProvidableCompositionLocal<IOSSwitchColors> = compositionLocalOf {
    error("No IOSSwitchColors provided")
}

public class IOSSwitchColors(
    public val checkedThumbColor: Color,
    public val checkedTrackColor: Color,
    public val normalThumbColor: Color,
    public val normalTrackColor: Color,
)