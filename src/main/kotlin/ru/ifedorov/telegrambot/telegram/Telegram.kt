package ru.ifedorov.telegrambot.telegram

import ru.ifedorov.telegrambot.data.db.DatabaseConnection
import ru.ifedorov.telegrambot.data.db.DatabaseUserDictionary
import ru.ifedorov.telegrambot.telegram.service.*
import ru.ifedorov.telegrambot.telegram.service.entity.Update
import ru.ifedorov.telegrambot.trainer.LearnWordsTrainer

fun main(args: Array<String>) {
    val botToken = args[0]
    val trainers = HashMap<Long, LearnWordsTrainer>()
    val telegramBotService = TelegramBotService(botToken)
    var updateId = 0L

    Runtime.getRuntime().addShutdownHook(Thread {
        DatabaseConnection.close()
    })

    while (true) {
        Thread.sleep(DELAY_MS)

        val response = telegramBotService.getUpdates(updateId) ?: continue
        println(response.result)

        val lastUpdate = response.result.lastOrNull() ?: continue
        updateId = lastUpdate.updateId + 1

        handleUpdate(lastUpdate, trainers, telegramBotService)
    }
}

fun handleUpdate(update: Update, trainers: HashMap<Long, LearnWordsTrainer>, botService: TelegramBotService) {
    val chatId = update.message?.chat?.id
        ?: update.callbackQuery?.message?.chat?.id
        ?: return

    val message = update.message?.text
    val data = update.callbackQuery?.data

    val trainer = trainers.getOrPut(chatId) {
        LearnWordsTrainer(DatabaseUserDictionary(chatId))
    }

    if (message == COMMAND_START || data == MENU_CALLBACK) {
        botService.sendMenu(chatId)
    }

    if (data == STATISTICS_CALLBACK) {
        val statistics = trainer.getStatistics()
        botService.sendMessage(
            chatId = chatId,
            message = "Выучено ${statistics.learnedCount} из ${statistics.totalCount} слов | ${statistics.percent}%"
        )
    }

    if (data == LEARN_WORDS_CALLBACK) {
        botService.checkNextQuestionAndSend(trainer, chatId)
    }

    data?.takeIf { it.startsWith(CALLBACK_DATA_ANSWER_PREFIX) }?.let {
        botService.checkAnswerAndSend(trainer, chatId, it)
    }

    if (data == RESET_CLICKED) {
        trainer.resetProgress()
        botService.sendMessage(chatId, "Прогресс сброшен")
    }
}