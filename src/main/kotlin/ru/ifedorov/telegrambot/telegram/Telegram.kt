package ru.ifedorov.telegrambot.telegram

import ru.ifedorov.telegrambot.data.db.DatabaseConnection
import ru.ifedorov.telegrambot.data.db.DatabaseUserDictionary
import ru.ifedorov.telegrambot.telegram.service.*
import ru.ifedorov.telegrambot.telegram.service.entity.GetFileResponse
import ru.ifedorov.telegrambot.telegram.service.entity.Update
import ru.ifedorov.telegrambot.trainer.LearnWordsTrainer
import java.io.File

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

    val username = update.message?.from?.username ?: ""
    val message = update.message?.text
    val data = update.callbackQuery?.data
    val document = update.message?.document

    val dictionary = DatabaseUserDictionary(chatId, username)
    val trainer = trainers.getOrPut(chatId) {
        LearnWordsTrainer(dictionary)
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

    if (data == LOAD_NEW_WORDS_CALLBACK) {
        botService.sendMessage(
            chatId,
            """
                Для загрузки новых слов с словарь отправьте в чат бота 
                текстовый файл формата txt, который содержит 
                "слово|перевод", с разделителем "|" например:
                
                cat|кошка
                dog|собака
                
                Можно добавлять несколько слов, каждое с новой строки.
            """.trimIndent()
        )
    }

    if (data == RESET_CLICKED) {
        trainer.resetProgress()
        botService.sendMessage(chatId, "Прогресс сброшен")
    }

    data?.takeIf { it.startsWith(CALLBACK_DATA_ANSWER_PREFIX) }?.let {
        botService.checkAnswerAndSend(trainer, chatId, it)
    }

    document?.let { document ->
        val fileInfoResponse: GetFileResponse? = botService.getFileInfoFromTelegram(document.fileId)

        if (fileInfoResponse?.ok == true && fileInfoResponse.result != null) {
            val filePath = fileInfoResponse.result.filePath
            val fileName = document.fileName

            botService.saveTelegramFileLocally(filePath, fileName)

            try {
                dictionary.updateDictionaryFromFile(File(fileName))
                botService.sendMessage(chatId, "Словарь успешно обновлен из файла $fileName")
            } catch (e: Exception) {
                botService.sendMessage(
                    chatId,
                    "Ошибка при обновлении словаря из файла. Проверьте формат файла и его содержание"
                )
                println("Не удалось обновить словарь из файла $fileName. Ошибка: ${e.message}")
            }

        } else {
            botService.sendMessage(chatId, "Ошибка при сохранении файла")
        }
    }
}