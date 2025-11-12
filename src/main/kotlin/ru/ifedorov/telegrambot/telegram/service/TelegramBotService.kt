package ru.ifedorov.telegrambot.telegram.service

import kotlinx.serialization.json.Json
import ru.ifedorov.telegrambot.telegram.service.entity.*
import ru.ifedorov.telegrambot.trainer.LearnWordsTrainer
import ru.ifedorov.telegrambot.trainer.model.Question
import java.io.File
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

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
    private val botToken: String
) {
    val client: HttpClient = HttpClient.newBuilder().build()
    val json = Json { ignoreUnknownKeys = true }
    private val sendMessageUrl = "$TELEGRAM_BASE_URL$botToken/sendMessage"

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
            println(result.exceptionOrNull()?.localizedMessage ?: "Some error")
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
            println(result.exceptionOrNull()?.localizedMessage ?: "Ошибка в getFileInfoFromTelegram()")
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
            println("Status code: ${response.statusCode()}")

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
                println("Файл $fileName успешно сохранен")
            }
            .onFailure { e ->
                println("Ошибка при сохранении файла: ${e.message}")
                e.printStackTrace()
            }
    }

    fun sendMessage(chatId: Long, message: String): String {
        val requestBody = SendMessageRequest(chatId, message)
        val requestBodyString = json.encodeToString<SendMessageRequest>(requestBody)
        return postJson(sendMessageUrl, requestBodyString)
    }

    fun sendMenu(chatId: Long): String {
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
        return postJson(sendMessageUrl, requestBodyString)
    }

    fun checkNextQuestionAndSend(trainer: LearnWordsTrainer, chatId: Long) {
        val nextQuestion = trainer.getNextQuestion()
        if (nextQuestion == null) {
            sendMessage(chatId, "Все слова в словаре выучены")
        } else {
            sendQuestion(chatId, nextQuestion)
        }
    }

    fun checkAnswerAndSend(trainer: LearnWordsTrainer, chatId: Long, data: String) {
        val userAnswerIndex = data.substringAfter(CALLBACK_DATA_ANSWER_PREFIX).toInt()
        val correctAnswer = trainer.getCurrentQuestion()?.correctAnswer

        if (correctAnswer != null) {
            val message = if (trainer.checkAnswer(userAnswerIndex)) {
                "Правильно!"
            } else {
                "Неправильно! ${correctAnswer.original} – это ${correctAnswer.translate}"
            }

            sendMessage(chatId, message)
            checkNextQuestionAndSend(trainer, chatId)
        }
    }

    private fun sendQuestion(chatId: Long, question: Question): String {
        val answerButtons = question.variants.mapIndexed { index, word ->
            listOf(
                InlineKeyboard(
                    text = word.translate,
                    callbackData = "$CALLBACK_DATA_ANSWER_PREFIX$index"
                )
            )
        }

        val menuButton = listOf(
            listOf(
                InlineKeyboard(
                    text = "Выйти в меню",
                    callbackData = MENU_CALLBACK
                )
            )
        )

        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = question.correctAnswer.original,
            replyMarkup = ReplyMarkup(answerButtons + menuButton)
        )
        val requestBodyString = json.encodeToString<SendMessageRequest>(requestBody)
        return postJson(sendMessageUrl, requestBodyString)
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
}