package com.github.mrbean355.zakbot.util

import org.commonmark.parser.Parser
import org.commonmark.renderer.text.TextContentRenderer

private val parser = Parser.builder().build()
private val renderer = TextContentRenderer.builder().build()
private val telegramMarkdownSpecialChars = Regex("([_*`\\[])")

/**
 * Converts a Markdown-formatted string into plain text.
 *
 * Markdown syntax will be removed from the string (e.g., `*bold*` becomes `bold`).
 * Escaped markdown characters will also be removed (e.g., `test\_user` becomes `test_user`).
 */
fun String.asPlainText(): String {
    return renderer.render(parser.parse(this))
}

/**
 * Escapes special characters for Telegram's legacy Markdown parse mode (`_`, `*`, `` ` ``, `[`).
 */
fun String.escapeTelegramMarkdown(): String {
    return replace(telegramMarkdownSpecialChars, "\\\\$1")
}

fun String.countOccurrences(word: String): Int =
    split(Regex("\\b")).count { it == word }
