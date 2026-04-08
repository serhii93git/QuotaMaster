package com.quotamaster.ui.home

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

class DragDropState(
    val lazyListState: LazyListState,
    internal var onMoveCallback: (Int, Int) -> Unit,
    internal var onDragEndCallback: () -> Unit
) {
    var draggedIndex by mutableIntStateOf(-1)
        private set
    var dragOffset by mutableFloatStateOf(0f)
        private set
    val isDragging: Boolean get() = draggedIndex >= 0

    fun onDragStart(index: Int) {
        draggedIndex = index
        dragOffset = 0f
    }

    fun onDrag(delta: Float) {
        dragOffset += delta
        checkSwap()
    }

    fun onDragFinished() {
        if (isDragging) {
            onDragEndCallback()
        }
        draggedIndex = -1
        dragOffset = 0f
    }

    fun onAutoScrolled(scrolled: Float) {
        dragOffset += scrolled
        checkSwap()
    }

    private fun checkSwap() {
        if (!isDragging) return
        val items = lazyListState.layoutInfo.visibleItemsInfo
        val current = items.firstOrNull { it.index == draggedIndex } ?: return
        val currentCenter = current.offset + current.size / 2 + dragOffset.toInt()

        items.firstOrNull { it.index == draggedIndex + 1 }?.let { next ->
            if (currentCenter > next.offset + next.size / 2) {
                onMoveCallback(draggedIndex, draggedIndex + 1)
                dragOffset -= next.size
                draggedIndex++
                return
            }
        }

        items.firstOrNull { it.index == draggedIndex - 1 }?.let { prev ->
            if (currentCenter < prev.offset + prev.size / 2) {
                onMoveCallback(draggedIndex, draggedIndex - 1)
                dragOffset += prev.size
                draggedIndex--
            }
        }
    }

    fun computeAutoScrollSpeed(): Float {
        if (!isDragging) return 0f
        val layoutInfo = lazyListState.layoutInfo
        val item = layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == draggedIndex } ?: return 0f

        val itemTop = item.offset + dragOffset
        val itemBottom = itemTop + item.size
        val viewStart = layoutInfo.viewportStartOffset.toFloat()
        val viewEnd = layoutInfo.viewportEndOffset.toFloat()
        val threshold = (viewEnd - viewStart) * 0.12f

        return when {
            itemTop < viewStart + threshold -> {
                -(viewStart + threshold - itemTop).coerceAtMost(20f)
            }
            itemBottom > viewEnd - threshold -> {
                (itemBottom - (viewEnd - threshold)).coerceAtMost(20f)
            }
            else -> 0f
        }
    }
}

@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    onMove: (Int, Int) -> Unit,
    onDragEnd: () -> Unit
): DragDropState {
    val state = remember {
        DragDropState(lazyListState, onMove, onDragEnd)
    }
    state.onMoveCallback = onMove
    state.onDragEndCallback = onDragEnd

    LaunchedEffect(Unit) {
        snapshotFlow { state.isDragging }
            .collectLatest { dragging ->
                if (dragging) {
                    while (state.isDragging) {
                        val speed = state.computeAutoScrollSpeed()
                        if (speed != 0f) {
                            val scrolled = lazyListState.scrollBy(speed)
                            state.onAutoScrolled(scrolled)
                        }
                        delay(16L)
                    }
                }
            }
    }

    return state
}