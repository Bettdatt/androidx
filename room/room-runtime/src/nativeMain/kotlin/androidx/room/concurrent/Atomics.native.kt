/*
 * Copyright 2025 The Android Open Source Project
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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.room.concurrent

import androidx.annotation.RestrictTo
import kotlin.concurrent.AtomicInt as KotlinAtomicInt

actual class AtomicInt actual constructor(initialValue: Int) {
    private val delegate: KotlinAtomicInt = KotlinAtomicInt(initialValue)

    actual fun get(): Int = delegate.value

    actual fun set(value: Int) {
        delegate.value = value
    }

    actual fun compareAndSet(expect: Int, update: Int): Boolean =
        delegate.compareAndSet(expect, update)

    actual fun incrementAndGet(): Int = delegate.incrementAndGet()

    actual fun getAndIncrement(): Int = delegate.getAndIncrement()

    actual fun decrementAndGet(): Int = delegate.decrementAndGet()
}

actual class AtomicBoolean actual constructor(initialValue: Boolean) {
    private val delegate: KotlinAtomicInt = KotlinAtomicInt(toInt(initialValue))

    actual fun get(): Boolean = delegate.value == 1

    actual fun compareAndSet(expect: Boolean, update: Boolean): Boolean =
        delegate.compareAndSet(toInt(expect), toInt(update))

    private fun toInt(value: Boolean) = if (value) 1 else 0
}
