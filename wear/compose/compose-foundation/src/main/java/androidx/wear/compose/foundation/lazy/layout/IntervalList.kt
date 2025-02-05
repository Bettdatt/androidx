/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.wear.compose.foundation.lazy.layout

import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf

/**
 * The list consisting of multiple intervals.
 *
 * It is useful when you want to define your own dsl similar to the one used by LazyColumn where
 * list items can be defined via multiple item/items calls.
 *
 * This interface is read only, in order to create a list you need to use [MutableIntervalList].
 *
 * @param T type of values each interval contains in [Interval.value].
 *
 * Note: this class is a part of [LazyLayout] harness that allows for building custom lazy layouts.
 * LazyLayout and all corresponding APIs are still under development and are subject to change.
 */
internal sealed interface IntervalList<out T> {

    /**
     * The total amount of items in all the intervals.
     *
     * Note that it is not the amount of intervals, but the sum of [Interval.size] for all the
     * intervals added into this list.
     */
    val size: Int

    /**
     * Returns the interval containing the given [index].
     *
     * @throws IndexOutOfBoundsException if the index is not within 0..[size] - 1 range.
     */
    operator fun get(index: Int): Interval<T>

    /**
     * Iterates through all the intervals starting from the one containing [fromIndex] until the one
     * containing [toIndex].
     *
     * @param fromIndex we will start iterating from the interval containing this index.
     * @param toIndex the last interval we iterate through will contain this index. This index
     *   should be not smaller than [fromIndex].
     * @param block will be invoked on each interval within the defined indexes
     * @throws IndexOutOfBoundsException if the indexes are not within 0..[size] - 1 range.
     */
    fun forEach(fromIndex: Int = 0, toIndex: Int = size - 1, block: (Interval<T>) -> Unit)

    /**
     * The interval holder.
     *
     * @see get
     */
    class Interval<out T>
    internal constructor(
        /** The index of the first item in the interval. */
        val startIndex: Int,
        /** The amount of items in the interval. */
        val size: Int,
        /** The value representing this interval. */
        val value: T
    ) {
        init {}
    }
}

/**
 * Mutable version of [IntervalList]. It allows you to add new intervals via [addInterval].
 *
 * Note: this class is a part of [LazyLayout] harness that allows for building custom lazy layouts.
 * LazyLayout and all corresponding APIs are still under development and are subject to change.
 */
internal class MutableIntervalList<T> : IntervalList<T> {
    private val intervals = mutableVectorOf<IntervalList.Interval<T>>()

    override var size = 0
        private set

    /**
     * Caches the last interval we binary searched for. We might not need to recalculate for
     * subsequent queries, as they tend to be localised.
     */
    private var lastInterval: IntervalList.Interval<T>? = null

    /**
     * Adds a new interval into this list.
     *
     * @param size the amount of items in the new interval.
     * @param value the value representing this interval.
     */
    fun addInterval(size: Int, value: T) {
        if (size == 0) {
            return
        }

        val interval = IntervalList.Interval(startIndex = this.size, size = size, value = value)
        this.size += size
        intervals.add(interval)
    }

    /**
     * Allows to iterate through all the intervals starting from the one containing [fromIndex]
     * until the one containing [toIndex].
     *
     * @param fromIndex we will start iterating from the interval containing this index.
     * @param toIndex the last interval we iterate through will contain this index. This index
     *   should be not smaller than [fromIndex].
     * @param block will be invoked on each interval within the defined indexes
     * @throws IndexOutOfBoundsException if the indexes are not within 0..[size] - 1 range.
     */
    override fun forEach(fromIndex: Int, toIndex: Int, block: (IntervalList.Interval<T>) -> Unit) {
        checkIndexBounds(fromIndex)
        checkIndexBounds(toIndex)

        var intervalIndex = intervals.binarySearch(fromIndex)
        var itemIndex = intervals[intervalIndex].startIndex
        while (itemIndex <= toIndex) {
            val interval = intervals[intervalIndex]
            block(interval)
            itemIndex += interval.size
            intervalIndex++
        }
    }

    override fun get(index: Int): IntervalList.Interval<T> {
        checkIndexBounds(index)
        return getIntervalForIndex(index)
    }

    private fun getIntervalForIndex(itemIndex: Int): IntervalList.Interval<T> {
        val lastInterval = lastInterval
        return if (lastInterval != null && lastInterval.contains(itemIndex)) {
            lastInterval
        } else {
            intervals[intervals.binarySearch(itemIndex)].also { this.lastInterval = it }
        }
    }

    @Suppress("NOTHING_TO_INLINE", "UNUSED_PARAMETER")
    private inline fun checkIndexBounds(index: Int) {}

    private fun IntervalList.Interval<T>.contains(index: Int) =
        index in startIndex until startIndex + size
}

/**
 * Finds the index of the interval which contains the highest value of
 * [IntervalList.Interval.startIndex] that is less than or equal to the given [itemIndex].
 */
private fun <T> MutableVector<IntervalList.Interval<T>>.binarySearch(itemIndex: Int): Int {
    var left = 0
    var right = lastIndex

    while (left < right) {
        val middle = left + (right - left) / 2

        val middleValue = get(middle).startIndex
        if (middleValue == itemIndex) {
            return middle
        }

        if (middleValue < itemIndex) {
            left = middle + 1

            // Verify that the left will not be bigger than our value
            if (itemIndex < get(left).startIndex) {
                return middle
            }
        } else {
            right = middle - 1
        }
    }

    return left
}
