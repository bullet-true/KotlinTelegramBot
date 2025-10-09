package org.example

import java.net.http.HttpClient

fun main(args: Array<String>) {

    val botToken = args[0]
    var updateId = 0
    val client: HttpClient = HttpClient.newBuilder().build()
    val telegramBotService = TelegramBotService(botToken, client)

    val updateIdRegex = "\"update_id\":(\\d+),".toRegex()
    val messageTextRegex = "\"text\":\"(.+?)\"".toRegex()
    val chatIdRegex = "\"chat\":\\{\"id\":(\\d+)".toRegex()
    val dataRegex = "\"data\":\"(.+?)\"".toRegex()

    while (true) {
        Thread.sleep(2000)
        val updates = telegramBotService.getUpdates(updateId)
        println(updates)

        val updateIdString = updateIdRegex
            .findAll(updates)
            .lastOrNull()
            ?.groupValues
            ?.getOrNull(1)

        updateId = updateIdString?.toInt()?.plus(1) ?: continue

        val chatId = chatIdRegex
            .findAll(updates)
            .lastOrNull()
            ?.groupValues
            ?.getOrNull(1)
            ?.toInt()

        val message = messageTextRegex
            .findAll(updates)
            .lastOrNull()
            ?.groupValues
            ?.getOrNull(1)

        val data = dataRegex
            .findAll(updates)
            .lastOrNull()
            ?.groupValues
            ?.getOrNull(1)

//        if (chatId != null && message != null) {
//            telegramBotService.sendMessage(chatId, message)
//        }

        if (chatId != null && message?.lowercase() == "/start") {
            telegramBotService.sendMenu(chatId)
        }

        if (chatId != null && data?.lowercase() == "statistics_clicked") {
            telegramBotService.sendMessage(chatId, "Выучено 10 из 10 слов | 100%")
        }
    }
}