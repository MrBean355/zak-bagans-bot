package com.github.mrbean355.zakbot.telegram

interface TelegramNotifier {

    fun sendMessage(text: String, actions: List<TelegramAction> = emptyList())
}
