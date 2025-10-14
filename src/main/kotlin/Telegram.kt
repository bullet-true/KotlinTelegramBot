package org.example

import java.net.http.HttpClient

fun main(args: Array<String>) {

    val botToken = args[0]
    var updateId = 0
    val client: HttpClient = HttpClient.newBuilder().build()
    val telegramBotService = TelegramBotService(botToken, client)
    val trainer = LearnWordsTrainer()

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
            ?.toInt() ?: continue

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

        if (message == COMMAND_START) {
            telegramBotService.sendMenu(chatId)
        }

        if (data == STATISTICS_CALLBACK) {
            val statistics = trainer.getStatistics()
            telegramBotService.sendMessage(
                chatId = chatId,
                message = "Выучено ${statistics.learnedCount} из ${statistics.totalCount} слов | ${statistics.percent}%"
            )
        }

        if (data == LEARN_WORDS_CALLBACK) {
            telegramBotService.checkNextQuestionAndSend(trainer, chatId)
        }

        data?.takeIf { it.startsWith(CALLBACK_DATA_ANSWER_PREFIX) }?.let {
            telegramBotService.checkAnswerAndSend(trainer, chatId, it)
        }
    }
}