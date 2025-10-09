package org.example

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

const val TELEGRAM_BASE_URL = "https://api.telegram.org/bot"

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
                            "callback_data": "learn_words_clicked"
                        },
                        {
                            "text": "Статистика",
                            "callback_data": "statistics_clicked"
                        }
                    ],
                    [
                        {
                            "text": "Выход",
                            "callback_data": "exit_clicked"
                        }
                    ]
                ]
            }
        }
    """.trimIndent()

        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(sendMenuBody))
            .build()

        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }
}