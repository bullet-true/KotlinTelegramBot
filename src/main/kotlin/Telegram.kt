package org.example

import java.net.http.HttpClient

fun main(args: Array<String>) {

    val botToken = args[0]
    var updateId = 0
    val client: HttpClient = HttpClient.newBuilder().build()
    val telegramBotService = TelegramBotService(botToken, client)

    while (true) {
        Thread.sleep(2000)
        val updates = telegramBotService.getUpdates(updateId)
        println(updates)

        val updateIdString = Regex("\"update_id\":(\\d+),")
            .findAll(updates)
            .lastOrNull()
            ?.groupValues
            ?.getOrNull(1)

        updateId = updateIdString?.toInt()?.plus(1) ?: continue

        val chatId = Regex("\"chat\":\\{\"id\":(\\d+)")
            .findAll(updates)
            .lastOrNull()
            ?.groupValues
            ?.getOrNull(1)
            ?.toInt()

        val messageText = Regex("\"text\":\"(.+?)\"")
            .findAll(updates)
            .lastOrNull()
            ?.groupValues
            ?.getOrNull(1)

        if (chatId != null && messageText != null) {
            println(telegramBotService.sendMessage(chatId, messageText))
        }
    }
}