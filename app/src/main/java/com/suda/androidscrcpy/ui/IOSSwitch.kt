package com.suda.androidscrcpy.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset

@Composable
public fun IOSSwitch(checkedState: State<Boolean>, onClick: (() -> Unit)?) {
    val config = LocalIOSSwitchConfig.current

    val clickModifier = remember(onClick) {
        if (onClick == null) {
            Modifier
        } else {
            Modifier.clickable(
                interactionSource = MutableInteractionSource(),
                onClick = onClick,
                indication = null,
            )
        }
    }

    Box(
        modifier = Modifier
            .width(config.width)
            .height(config.trackHeight)
            .clip(CircleShape)
            .then(clickModifier),
        contentAlignment = Alignment.CenterStart,
    ) {
        CPTrack(checkedState)
        CPThumb(checkedState)
    }
}

private fun getTrackColor(checkedState: State<Boolean>, colors: IOSSwitchColors): Color {
    return if (checkedState.value) colors.checkedTrackColor else colors.normalTrackColor
}

@Composable
private fun CPTrack(checkedState: State<Boolean>) {
    val colors = LocalIOSSwitchColors.current

    val targetColorState = remember(checkedState) {
        derivedStateOf { getTrackColor(checkedState, colors) }
    }

    val trackColorState = animateColorAsState(
        targetValue = targetColorState.value,
        animationSpec = tween(),
        label = "TrackColor",
    )

    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .background(trackColorState.value),
    )
}

private fun getThumbPaddingStart(checkedState: State<Boolean>, config: IOSSwitchConfig): Dp {
    return if (checkedState.value) {
        config.width - config.trackPadding - config.thumbSize
    } else {
        config.trackPadding
    }
}

private fun getThumbColor(checkedState: State<Boolean>, colors: IOSSwitchColors): Color {
    return if (checkedState.value) colors.checkedThumbColor else colors.normalThumbColor
}

@Composable
private fun CPThumb(checkedState: State<Boolean>) {
    val colors = LocalIOSSwitchColors.current
    val config = LocalIOSSwitchConfig.current

    val targetPaddingStartState = remember(checkedState, config) {
        derivedStateOf { getThumbPaddingStart(checkedState, config) }
    }

    val targetThumbColorState = remember(checkedState, colors) {
        derivedStateOf { getThumbColor(checkedState, colors) }
    }

    val thumbPaddingStartState = animateDpAsState(
        targetValue = targetPaddingStartState.value,
        animationSpec = tween(),
        label = "ThumbPadding",
    )

    val thumbColorState = animateColorAsState(
        targetValue = targetThumbColorState.value,
        animationSpec = tween(),
        label = "ThumbColor",
    )

    Spacer(
        modifier = Modifier
            .offset { IntOffset(thumbPaddingStartState.value.roundToPx(), 0) }
            .size(config.thumbSize)
            .clip(CircleShape)
            .background(thumbColorState.value),
    )
}