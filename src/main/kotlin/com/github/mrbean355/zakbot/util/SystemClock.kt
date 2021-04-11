package com.github.mrbean355.zakbot.util

import org.springframework.stereotype.Component

@Component
class SystemClock {
    val currentTimeMillis: Long get() = System.currentTimeMillis()
}