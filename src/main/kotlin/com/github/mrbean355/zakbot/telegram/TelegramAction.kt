package com.github.mrbean355.zakbot.telegram

sealed interface TelegramAction {
    data class OpenUrl(val url: String) : TelegramAction
    data class IgnoreUser(val username: String) : TelegramAction
    data class UnignoreUser(val username: String) : TelegramAction
    data class IgnoreSubmission(val fullName: String) : TelegramAction
    data class UnignoreSubmission(val fullName: String) : TelegramAction
    data class DeleteComment(val fullName: String) : TelegramAction
    data object RefreshStatus : TelegramAction
}
