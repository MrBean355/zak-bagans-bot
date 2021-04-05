package com.github.mrbean355.zakbot

import java.io.File

private val File = File("cache.txt").also {
    it.createNewFile()
}

fun getLastChecked(): Long = synchronized(File) {
    File.readText().toLongOrNull() ?: 0
}

fun setLastChecked(time: Long): Unit = synchronized(File) {
    File.writeText(time.toString())
}