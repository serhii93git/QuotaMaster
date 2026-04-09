package com.quotamaster.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DragDropState(
    val lazyListState: LazyListState,
    internal var onMoveCallback: (Int, Int) -> Unit,
    internal var onDragEndCallback: () -> Unit,
    private val scope: CoroutineScope
) {
    var draggedIndex by mutableIntStateOf(-1)
        private set

    /** Animated offset — drives visual position of dragged card. */
    val dragOffsetAnimatable = Animatable(0f)
    val dragOffset: Float get() = dragOffsetAnimatable.value

    var itemCount: Int = 0
    val isDragging: Boolean get() = draggedIndex >= 0

    /** Minimum drag distance before a swap is considered (prevents jitter). */
    private val swapDeadZone = 8f

    /** Last swap direction to add hysteresis. */
    private var lastSwapDirection = 0 // -1 = up, 1 = down, 0 = none

    fun onDragStart(index: Int) {
        if (index < 0 || index >= itemCount) return
        draggedIndex = index
        lastSwapDirection = 0
        scope.launch { dragOffsetAnimatable.snapTo(0f) }
    }

    fun onDrag(delta: Float) {
        if (!isDragging) return
        scope.launch { dragOffsetAnimatable.snapTo(dragOffset + delta) }
        checkSwap()
    }

    fun onDragFinished() {
        if (isDragging) {
            onDragEndCallback()
        }
        val wasDragging = isDragging
        draggedIndex = -1
        lastSwapDirection = 0

        if (wasDragging) {
            // Animate snap-back to 0
            scope.launch {
                dragOffsetAnimatable.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        }
    }

    fun onAutoScrolled(scrolled: Float) {
        if (!isDragging) return
        scope.launch { dragOffsetAnimatable.snapTo(dragOffset + scrolled) }
        checkSwap()
    }

    private fun checkSwap() {
        if (!isDragging) return
        if (kotlin.math.abs(dragOffset) < swapDeadZone) return

        val items = lazyListState.layoutInfo.visibleItemsInfo
        val current = items.firstOrNull { it.index == draggedIndex } ?: return
        val currentCenter = current.offset + current.size / 2 + dragOffset.toInt()

        // Swap down — require 60% past midpoint (hysteresis)
        val nextIndex = draggedIndex + 1
        if (nextIndex < itemCount && lastSwapDirection != -1) {
            items.firstOrNull { it.index == nextIndex }?.let { next ->
                val nextThreshold = next.offset + (next.size * 0.6f).toInt()
                if (currentCenter > nextThreshold) {
                    onMoveCallback(draggedIndex, nextIndex)
                    scope.launch { dragOffsetAnimatable.snapTo(dragOffset - next.size) }
                    draggedIndex = nextIndex
                    lastSwapDirection = 1
                    return
                }
            }
        }

        // Swap up — require 60% past midpoint
        val prevIndex = draggedIndex - 1
        if (prevIndex >= 0 && lastSwapDirection != 1) {
            items.firstOrNull { it.index == prevIndex }?.let { prev ->
                val prevThreshold = prev.offset + (prev.size * 0.4f).toInt()
                if (currentCenter < prevThreshold) {
                    onMoveCallback(draggedIndex, prevIndex)
                    scope.launch { dragOffsetAnimatable.snapTo(dragOffset + prev.size) }
                    draggedIndex = prevIndex
                    lastSwapDirection = -1
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
                -(viewStart + threshold - itemTop).coerceAtMost(12f)
            }
            itemBottom > viewEnd - threshold -> {
                (itemBottom - (viewEnd - threshold)).coerceAtMost(12f)
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
    val scope = rememberCoroutineScope()
    val state = remember {
        DragDropState(lazyListState, onMove, onDragEnd, scope)
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