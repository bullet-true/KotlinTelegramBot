package ru.ifedorov.telegrambot.telegram.service

import kotlinx.serialization.json.Json
import ru.ifedorov.telegrambot.data.db.DatabaseUserDictionaryRepository
import ru.ifedorov.telegrambot.telegram.service.entity.*
import ru.ifedorov.telegrambot.trainer.LearnWordsTrainer
import java.io.File
import java.io.InputStream
import java.math.BigInteger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.logging.Logger

const val TELEGRAM_BASE_URL = "https://api.telegram.org/bot"
const val BOT_FILE_URL = "https://api.telegram.org/file/bot"
const val LEARN_WORDS_CALLBACK = "learn_words_clicked"
const val STATISTICS_CALLBACK = "statistics_clicked"
const val LOAD_NEW_WORDS_CALLBACK = "load_new_words_clicked"
const val MENU_CALLBACK = "menu_clicked"
const val RESET_CLICKED = "reset_clicked"
const val COMMAND_START = "/start"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"
const val DELAY_MS = 2000L

class TelegramBotService(
    private val botToken: String,
    val dictionaryRepository: DatabaseUserDictionaryRepository,
    val dynamicMessage: DynamicMessage,
    val dynamicPhoto: DynamicPhoto
) {
    private val client: HttpClient = HttpClient.newBuilder().build()
    private val json = Json { ignoreUnknownKeys = true }
    private val sendMessageUrl = "$TELEGRAM_BASE_URL$botToken/sendMessage"
    private val logger = Logger.getLogger(TelegramBotService::class.java.name)

    fun getUpdates(updateId: Long): Response? {
        val url = "$TELEGRAM_BASE_URL$botToken/getUpdates?offset=$updateId"
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .build()

        val result = runCatching {
            val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
            val responseString = response.body()
            json.decodeFromString<Response>(responseString)
        }

        if (result.isFailure) {
            logger.warning(result.exceptionOrNull()?.localizedMessage ?: "Some error")
        }

        return result.getOrNull()
    }

    fun getFileInfoFromTelegram(fileId: String): GetFileResponse? {
        val url = "$TELEGRAM_BASE_URL$botToken/getFile"
        val requestBody = GetFileRequest(fileId = fileId)
        val requestBodyString = json.encodeToString<GetFileRequest>(requestBody)

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()

        val result = runCatching {
            val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
            val responseString = response.body()
            json.decodeFromString<GetFileResponse>(responseString)
        }

        if (result.isFailure) {
            logger.warning(result.exceptionOrNull()?.localizedMessage ?: "Ошибка в getFileInfoFromTelegram()")
        }

        return result.getOrNull()
    }

    fun saveTelegramFileLocally(filePath: String, fileName: String) {
        val url = "$BOT_FILE_URL$botToken/$filePath"

        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .build()

        runCatching {
            val response: HttpResponse<InputStream> = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
            logger.info("Status code: ${response.statusCode()}")

            if (response.statusCode() != 200) {
                error("Ошибка HTTP ${response.statusCode()}")
            }

            response.body().use { inputStream ->
                File(fileName).outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream, 16 * 1024)
                }
            }
        }
            .onSuccess {
                logger.info("Файл $fileName успешно сохранен")
            }
            .onFailure { e ->
                logger.info("Ошибка при сохранении файла: ${e.message}")
                e.printStackTrace()
            }
    }

    fun sendMessage(chatId: Long, message: String): String {
        val requestBody = SendMessageRequest(chatId, message)
        val requestBodyString = json.encodeToString<SendMessageRequest>(requestBody)
        return postJson(sendMessageUrl, requestBodyString)
    }

    fun sendMenu(chatId: Long): String {
        deleteLastMessage(chatId)
        deleteLastPhoto(chatId)

        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = "Основное меню",
            replyMarkup = ReplyMarkup(
                listOf(
                    listOf(
                        InlineKeyboard(text = "Изучить слова", callbackData = LEARN_WORDS_CALLBACK),
                        InlineKeyboard(text = "Статистика", callbackData = STATISTICS_CALLBACK),
                    ),
                    listOf(
                        InlineKeyboard(text = "Загрузка новых слов в словарь", callbackData = LOAD_NEW_WORDS_CALLBACK)
                    ),
                    listOf(
                        InlineKeyboard(text = "Сбросить прогресс", callbackData = RESET_CLICKED),
                    )
                )
            )
        )
        val requestBodyString = json.encodeToString<SendMessageRequest>(requestBody)
        val responseString = postJson(sendMessageUrl, requestBodyString)

        val messageResponse = runCatching {
            json.decodeFromString<SendMessageResponse>(responseString)
        }.getOrNull()

        messageResponse?.result?.messageId?.let {
            dynamicMessage.saveMessageId(chatId, it)
        }

        return responseString
    }


    fun checkAnswerAndSend(trainer: LearnWordsTrainer, chatId: Long, data: String) {
        val userAnswerIndex = data.substringAfter(CALLBACK_DATA_ANSWER_PREFIX).toInt()
        val correctAnswer = trainer.getCurrentQuestion()?.correctAnswer ?: return
        val isCorrect = trainer.checkAnswer(userAnswerIndex)

        val resultText = if (isCorrect) {
            "Правильно!\n\n"
        } else {
            "Неправильно!\n${correctAnswer.original} – это ${correctAnswer.translate}\n\n"
        }

        checkNextQuestionAndSend(trainer, chatId, resultText)
    }

    fun editMessageWithKeyboard(
        chatId: Long,
        messageId: Long,
        text: String,
        replyMarkup: ReplyMarkup? = null,
        parseMode: String? = "HTML"
    ): Boolean {
        val url = "$TELEGRAM_BASE_URL$botToken/editMessageText"
        val requestBody = EditMessageRequest(chatId, messageId, text, parseMode, replyMarkup)
        val requestBodyString = json.encodeToString<EditMessageRequest>(requestBody)

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()

        return runCatching {
            val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
            val responseBody = response.body()
            json.decodeFromString<EditMessageResponse>(responseBody).ok
        }.onFailure { e ->
            logger.warning("Ошибка при редактировании сообщения с помощью editMessageText: ${e.message}")
        }.getOrElse { false }
    }

    fun sendDynamicMessage(
        chatId: Long,
        text: String,
        replyMarkup: ReplyMarkup? = null,
        withBackButton: Boolean = false,
        parseMode: String? = "HTML"
    ): Long? {
        deleteLastMessage(chatId)

        val finalReplyMarkup = if (withBackButton) {
            ReplyMarkup(
                listOf(listOf(InlineKeyboard("Назад в меню", MENU_CALLBACK)))
            )
        } else {
            replyMarkup
        }

        val request = SendMessageRequest(chatId, text, finalReplyMarkup, parseMode)
        val responseString = postJson(sendMessageUrl, json.encodeToString(request))

        val messageId = runCatching {
            json.decodeFromString<SendMessageResponse>(responseString).result?.messageId
        }.getOrNull()

        messageId?.let { dynamicMessage.saveMessageId(chatId, it) }
        return messageId
    }

    fun checkNextQuestionAndSend(trainer: LearnWordsTrainer, chatId: Long, resultPrefix: String? = null) {
        val question = trainer.getNextQuestion()

        if (question == null) {
            deleteLastPhoto(chatId)
            val message = if (resultPrefix != null) {
                resultPrefix + "Все слова выучены"
            } else {
                "Все слова в словаре выучены"
            }
            sendDynamicMessage(chatId, message)
            return
        }

        deleteLastPhoto(chatId)
        sendWordImage(question.correctAnswer.original, chatId)

        val baseText = "Выбери правильный перевод для слова:\n${question.correctAnswer.original}"
        val text = if (resultPrefix != null) resultPrefix + baseText else baseText

        val answerButtons = question.variants.mapIndexed { index, word ->
            listOf(InlineKeyboard(word.translate, "$CALLBACK_DATA_ANSWER_PREFIX$index"))
        }

        val menuButton = listOf(listOf(InlineKeyboard("Выйти в меню", MENU_CALLBACK)))
        val markup = ReplyMarkup(answerButtons + menuButton)

        sendDynamicMessage(chatId, text, markup)
    }

    private fun deleteLastMessage(chatId: Long) {
        dynamicMessage.getMessageId(chatId)?.let { messageId ->
            deleteMessage(chatId, messageId)
            dynamicMessage.removeMessageId(chatId)
        }
    }

    private fun deleteLastPhoto(chatId: Long) {
        dynamicPhoto.getPhotoMessageId(chatId)?.let { photoId ->
            deleteMessage(chatId, photoId)
            dynamicPhoto.removePhotoMessageId(chatId)
        }
    }

    private fun deleteMessage(chatId: Long, messageId: Long): Boolean {
        val url = "$TELEGRAM_BASE_URL$botToken/deleteMessage"
        val body = """
            {
                "chat_id": $chatId, 
                "message_id": $messageId
            }
        """.trimIndent()

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        return runCatching {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            json.decodeFromString<DeleteMessageResponse>(response.body()).ok
        }.getOrDefault(false)
    }

    private fun sendDynamicPhoto(chatId: Long, file: File, hasSpoiler: Boolean = true): String {
        deleteLastPhoto(chatId)

        val responseString = sendPhoto(file, chatId, hasSpoiler)

        val sendPhotoResponse = runCatching {
            json.decodeFromString<SendPhotoResponse>(responseString)
        }.getOrNull()

        val messageId = sendPhotoResponse?.result?.messageId
        messageId?.let { dynamicPhoto.savePhotoMessageId(chatId, it) }

        return responseString
    }

    private fun postJson(url: String, body: String): String {
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    private fun sendPhoto(file: File, chatId: Long, hasSpoiler: Boolean = true): String {
        val data: MutableMap<String, Any> = LinkedHashMap()
        data["chat_id"] = chatId.toString()
        data["photo"] = file
        data["has_spoiler"] = hasSpoiler
        val boundary: String = BigInteger(35, Random()).toString()

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$TELEGRAM_BASE_URL$botToken/sendPhoto"))
            .postMultipartFormData(boundary, data)
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    private fun HttpRequest.Builder.postMultipartFormData(
        boundary: String,
        data: Map<String, Any>
    ): HttpRequest.Builder {

        val byteArrays = ArrayList<ByteArray>()
        val separator = "--$boundary\r\nContent-Disposition: form-data; name=".toByteArray(StandardCharsets.UTF_8)

        for (entry in data.entries) {
            byteArrays.add(separator)
            when (val value = entry.value) {
                is File -> {
                    val path = Path.of(value.toURI())
                    val mimeType = Files.probeContentType(path)
                    byteArrays.add(
                        "\"${entry.key}\"; filename=\"${path.fileName}\"\r\nContent-Type: $mimeType\r\n\r\n"
                            .toByteArray(StandardCharsets.UTF_8)
                    )
                    byteArrays.add(Files.readAllBytes(path))
                    byteArrays.add("\r\n".toByteArray(StandardCharsets.UTF_8))
                }

                else -> byteArrays.add("\"${entry.key}\"\r\n\r\n${entry.value}\r\n".toByteArray(StandardCharsets.UTF_8))
            }
        }
        byteArrays.add("--$boundary--".toByteArray(StandardCharsets.UTF_8))

        this.header("Content-Type", "multipart/form-data;boundary=$boundary")
            .POST(HttpRequest.BodyPublishers.ofByteArrays(byteArrays))
        return this
    }

    private fun getFileIdFromSendPhotoResponse(responseString: String): String? {
        val result = runCatching {
            json.decodeFromString<SendPhotoResponse>(responseString)
        }

        if (result.isFailure) {
            logger.warning(result.exceptionOrNull()?.localizedMessage ?: "Error in getFileIdFromSendPhotoResponse")
            return null
        }

        return result.getOrNull()?.result?.photo?.lastOrNull()?.fileId
    }

    private fun sendPhotoByFileId(fileId: String, chatId: Long, hasSpoiler: Boolean = true): String {
        val body = """
            {
                "chat_id": $chatId,
                "photo": "$fileId",
                "has_spoiler": $hasSpoiler               
            }
        """.trimIndent()

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$TELEGRAM_BASE_URL$botToken/sendPhoto"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        return response.body()
    }

    private fun sendWordImage(word: String, chatId: Long) {
        val wordId = dictionaryRepository.getIdForWord(word) ?: return

        val telegramFileId = dictionaryRepository.getTelegramFileIdForWord(wordId)
        var imagePath = dictionaryRepository.getImagePathForWord(wordId)

        if (imagePath == null) {
            val dir = File("images")
            if (!dir.exists()) {
                val created = dir.mkdir()
                if (!created) {
                    logger.warning("Не удалось создать папку images")
                    return
                }
            }

            val file = dir.listFiles()?.firstOrNull {
                it.name.substringBeforeLast('.').equals(word, ignoreCase = true)
            } ?: return

            dictionaryRepository.saveImagePathForWord(wordId, file.path)
            imagePath = file.path
        }

        val file = File(imagePath)
        if (!file.exists()) return

        deleteLastPhoto(chatId)

        if (!telegramFileId.isNullOrBlank()) {
            val fileIdResponse = sendPhotoByFileId(telegramFileId, chatId)
            logger.info("Фото отправлено по file_id: \n$fileIdResponse")

            val messageId = runCatching {
                json.decodeFromString<SendPhotoResponse>(fileIdResponse).result?.messageId
            }.getOrNull()
            messageId?.let { dynamicPhoto.savePhotoMessageId(chatId, it) }

            return
        }

        val multipartResponse = sendDynamicPhoto(chatId, file)
        logger.info("Фото отправлено как файл через multipart запрос: \n$multipartResponse")

        val messageId = runCatching {
            json.decodeFromString<SendPhotoResponse>(multipartResponse).result?.messageId
        }.getOrNull()
        messageId?.let { dynamicPhoto.savePhotoMessageId(chatId, it) }

        val fileId = getFileIdFromSendPhotoResponse(multipartResponse)
        if (!fileId.isNullOrBlank()) {
            dictionaryRepository.saveTelegramFileIdForWord(wordId, fileId)
        } else {
            logger.warning("file_id не найден в multipartResponse")
        }
    }
}