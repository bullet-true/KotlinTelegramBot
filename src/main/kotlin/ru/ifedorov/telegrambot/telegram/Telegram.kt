package ru.ifedorov.telegrambot.telegram

import ru.ifedorov.telegrambot.data.db.DatabaseConnection
import ru.ifedorov.telegrambot.data.db.DatabaseUserDictionaryRepository
import ru.ifedorov.telegrambot.telegram.service.*
import ru.ifedorov.telegrambot.telegram.service.entity.GetFileResponse
import ru.ifedorov.telegrambot.telegram.service.entity.Update
import ru.ifedorov.telegrambot.trainer.LearnWordsTrainer
import java.io.File
import java.util.logging.Logger

fun main(args: Array<String>) {
    val botToken = args[0]
    val trainers = HashMap<Long, LearnWordsTrainer>()
    val telegramBotService: TelegramBotService = TelegramBotService(
        botToken = botToken,
        dictionaryRepository = DatabaseUserDictionaryRepository(),
        dynamicMessage = DynamicMessage(),
        dynamicPhoto = DynamicPhoto()
    )
    val logger = Logger.getLogger("TelegramBotMain")
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

        try {
            handleUpdate(lastUpdate, trainers, telegramBotService)
        } catch (e: Exception) {
            logger.warning("Exception: ${e.localizedMessage}")
            e.stackTrace
        }
    }
}

fun handleUpdate(
    update: Update,
    trainers: HashMap<Long, LearnWordsTrainer>,
    botService: TelegramBotService
) {
    val chatId = update.message?.chat?.id
        ?: update.callbackQuery?.message?.chat?.id
        ?: return

    val dictionary = botService.dictionaryRepository
    val username = update.message?.from?.username ?: ""
    val message = update.message?.text
    val data = update.callbackQuery?.data
    val document = update.message?.document


    val trainer = trainers.getOrPut(chatId) {
        LearnWordsTrainer(dictionary, chatId, username)
    }

    if (message == COMMAND_START || data == MENU_CALLBACK) {
        botService.sendMenu(chatId)
    }

    if (data == STATISTICS_CALLBACK) {
        botService.sendOrUpdateStatistics(trainer, chatId)
    }

    if (data == LEARN_WORDS_CALLBACK) {
        botService.checkNextQuestionAndSend(trainer, chatId)
    }

    if (data == LOAD_NEW_WORDS_CALLBACK) {
        val text = """
            Для загрузки новых слов в словарь отправьте текстовый файл формата txt с разделителем "|", например:
            
            cat|кошка
            dog|собака
            
            Каждое слово с новой строки.
        """.trimIndent()
        botService.sendDynamicMessage(chatId, text, withBackButton = true)
    }

    if (data == RESET_CLICKED) {
        trainer.resetProgress()
        botService.sendOrUpdateStatistics(trainer, chatId)
        botService.sendDynamicMessage(chatId, "Прогресс сброшен", withBackButton = true)
    }

    if (message == "/undo") {
        val prevText = botService.dynamicMessage.undoStatisticsText(chatId)
        prevText?.let { text ->
            botService.dynamicMessage.getStatisticsMessageId(chatId)?.let { messageId ->
                botService.editMessageWithKeyboard(chatId, messageId, text)
            }
        }
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
                botService.sendDynamicMessage(
                    chatId,
                    "Словарь успешно обновлён из файла $fileName",
                    withBackButton = true
                )

            } catch (e: Exception) {
                botService.sendDynamicMessage(
                    chatId,
                    "Ошибка при обновлении словаря. Проверьте формат файла $fileName и его содержимое. Ошибка: ${e.message}",
                    withBackButton = true
                )
            }

        } else {
            botService.sendDynamicMessage(chatId, "Ошибка при сохранении файла", withBackButton = true)
        }
    }
}