package com.duychien.fixmate.core.common

import javax.inject.Inject

/** Tiny time source so repository logic can be tested deterministically. */
fun interface Clock {
    fun now(): Long
}

class SystemClock @Inject constructor() : Clock {
    override fun now(): Long = System.currentTimeMillis()
}
