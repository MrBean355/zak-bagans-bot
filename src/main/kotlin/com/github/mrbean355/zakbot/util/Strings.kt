package com.github.mrbean355.zakbot.util

import org.slf4j.LoggerFactory
import java.util.ResourceBundle

private val strings = ResourceBundle.getBundle("strings")

fun getString(key: String): String {
    return if (strings.containsKey(key)) {
        strings.getString(key)
    } else {
        LoggerFactory.getLogger("StringsKt").error("String not found: $key")
        key
    }
}

fun getString(key: String, vararg formatArgs: Any?): String {
    return getString(key).format(*formatArgs)
}