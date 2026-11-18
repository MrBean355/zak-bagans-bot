package com.github.mrbean355.zakbot.util

import org.springframework.context.MessageSource
import java.util.Locale

fun MessageSource.getString(key: String, vararg args: Any?): String {
    @Suppress("UNCHECKED_CAST")
    return getMessage(key, args as Array<out Any>, Locale.ENGLISH)
}
