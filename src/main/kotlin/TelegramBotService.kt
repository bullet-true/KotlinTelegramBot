package org.example

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

const val TELEGRAM_BASE_URL = "https://api.telegram.org/bot"
const val LEARN_WORDS_CALLBACK = "learn_words_clicked"
const val STATISTICS_CALLBACK = "statistics_clicked"
const val EXIT_CALLBACK = "exit_clicked"
const val COMMAND_START = "/start"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"

class TelegramBotService(private val botToken: String, private val client: HttpClient) {

    fun getUpdates(updateId: Int): String {
        val url = "$TELEGRAM_BASE_URL$botToken/getUpdates?offset=$updateId"
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .build()

        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendMessage(chatId: Int, message: String): String {
        val encodedText = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
        val url = "$TELEGRAM_BASE_URL$botToken/sendMessage?chat_id=$chatId&text=$encodedText"
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .build()

        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendMenu(chatId: Int): String {
        val url = "$TELEGRAM_BASE_URL$botToken/sendMessage"
        val sendMenuBody = """
        {
            "chat_id": $chatId,
            "text": "Основное меню",
            "reply_markup": {
                "inline_keyboard": [
                    [
                        {
                            "text": "Изучить слова",
                            "callback_data": "$LEARN_WORDS_CALLBACK"
                        },
                        {
                            "text": "Статистика",
                            "callback_data": "$STATISTICS_CALLBACK"
                        }
                    ],
                    [
                        {
                            "text": "Выход",
                            "callback_data": "$EXIT_CALLBACK"
                        }
                    ]
                ]
            }
        }
    """.trimIndent()

        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(sendMenuBody))
            .build()

        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    private fun sendQuestion(chatId: Int, question: Question): String {
        val buttons = question.variants.mapIndexed { index, word ->
            "[{\"text\":\"${word.translate}\",\"callback_data\":\"${CALLBACK_DATA_ANSWER_PREFIX}${index}\"}]"
        }.joinToString(",")

        val sendQuestinBody = """
        {
            "chat_id": $chatId,
            "text": "${question.correctAnswer.original}",
            "reply_markup": {
                "inline_keyboard": [
                    $buttons
                ]
            }
        }
        """.trimIndent()

        val url = "$TELEGRAM_BASE_URL$botToken/sendMessage"
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(sendQuestinBody))
            .build()

        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun checkNextQuestionAndSend(
        trainer: LearnWordsTrainer,
        telegramBotService: TelegramBotService,
        chatId: Int
    ) {
        val nextQuestion = trainer.getNextQuestion()
        if (nextQuestion == null) {
            telegramBotService.sendMessage(chatId, "Все слова в словаре выучены")
        } else {
            telegramBotService.sendQuestion(chatId, nextQuestion)
        }
    }
}