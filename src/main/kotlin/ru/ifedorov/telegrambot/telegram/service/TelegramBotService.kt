package ru.ifedorov.telegrambot.telegram.service

import ru.ifedorov.telegrambot.telegram.service.entity.InlineKeyboard
import ru.ifedorov.telegrambot.telegram.service.entity.ReplyMarkup
import ru.ifedorov.telegrambot.telegram.service.entity.Response
import ru.ifedorov.telegrambot.telegram.service.entity.SendMessageRequest
import ru.ifedorov.telegrambot.trainer.LearnWordsTrainer
import ru.ifedorov.telegrambot.trainer.model.Question
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

const val TELEGRAM_BASE_URL = "https://api.telegram.org/bot"
const val LEARN_WORDS_CALLBACK = "learn_words_clicked"
const val STATISTICS_CALLBACK = "statistics_clicked"
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