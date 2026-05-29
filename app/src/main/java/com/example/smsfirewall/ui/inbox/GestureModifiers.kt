package com.example.smsfirewall.ui.inbox

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

private val SWIPE_BACK_THRESHOLD_DP = 96.dp

internal fun Modifier.swipeBackGesture(onBack: () -> Unit): Modifier = composed {
    val swipeBackThresholdPx = with(LocalDensity.current) { SWIPE_BACK_THRESHOLD_DP.toPx() }
    val haptic = LocalHapticFeedback.current
    pointerInput(onBack, swipeBackThresholdPx) {
        var draggedDistance = 0f
        var backTriggered = false
        var hapticTriggered = false

        detectHorizontalDragGestures(
            onDragStart = {
                draggedDistance = 0f
                backTriggered = false
                hapticTriggered = false
            },
            onHorizontalDrag = { change, dragAmount ->
                if (dragAmount > 0f) {
                    draggedDistance += dragAmount
                }
                if (!hapticTriggered && draggedDistance >= swipeBackThresholdPx * 0.8f) {
                    hapticTriggered = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                if (!backTriggered && draggedDistance >= swipeBackThresholdPx) {
                    backTriggered = true
                    change.consume()
                    onBack()
                }
            },
            onDragEnd = {
                draggedDistance = 0f
                backTriggered = false
                hapticTriggered = false
            },
            onDragCancel = {
                draggedDistance = 0f
                backTriggered = false
                hapticTriggered = false
            }
        )
    }
}
