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
    var itemCount: Int = 0
    val isDragging: Boolean get() = draggedIndex >= 0

    fun onDragStart(index: Int) {
        if (index < 0 || index >= itemCount) return
        draggedIndex = index
        dragOffset = 0f
    }

    fun onDrag(delta: Float) {
        if (!isDragging) return
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
        if (!isDragging) return
        dragOffset += scrolled
        checkSwap()
    }

    private fun checkSwap() {
        if (!isDragging) return
        val items = lazyListState.layoutInfo.visibleItemsInfo
        val current = items.firstOrNull { it.index == draggedIndex } ?: return
        val currentCenter = current.offset + current.size / 2 + dragOffset.toInt()

        // Try swap down — only if next index is within card bounds
        val nextIndex = draggedIndex + 1
        if (nextIndex < itemCount) {
            items.firstOrNull { it.index == nextIndex }?.let { next ->
                if (currentCenter > next.offset + next.size / 2) {
                    onMoveCallback(draggedIndex, nextIndex)
                    dragOffset -= next.size
                    draggedIndex = nextIndex
                    return
                }
            }
        }

        // Try swap up — only if prev index is valid
        val prevIndex = draggedIndex - 1
        if (prevIndex >= 0) {
            items.firstOrNull { it.index == prevIndex }?.let { prev ->
                if (currentCenter < prev.offset + prev.size / 2) {
                    onMoveCallback(draggedIndex, prevIndex)
                    dragOffset += prev.size
                    draggedIndex = prevIndex
                }
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
        val threshold = (viewEnd - viewStart) * 0.15f

        return when {
            itemTop < viewStart + threshold -> {
                -(viewStart + threshold - itemTop).coerceAtMost(15f)
            }
            itemBottom > viewEnd - threshold -> {
                (itemBottom - (viewEnd - threshold)).coerceAtMost(15f)
            }
            else -> 0f
        }
    }
}

@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    itemCount: Int,
    onMove: (Int, Int) -> Unit,
    onDragEnd: () -> Unit
): DragDropState {
    val state = remember {
        DragDropState(lazyListState, onMove, onDragEnd)
    }
    state.onMoveCallback = onMove
    state.onDragEndCallback = onDragEnd
    state.itemCount = itemCount

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