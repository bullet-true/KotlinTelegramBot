package org.example

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.http.HttpClient

fun main(args: Array<String>) {

    val botToken = args[0]
    val client: HttpClient = HttpClient.newBuilder().build()
    val json = Json { ignoreUnknownKeys = true }
    val telegramBotService = TelegramBotService(botToken, client, json)
    val trainer = LearnWordsTrainer()
    var updateId = 0L

    while (true) {
        Thread.sleep(DELAY_MS)

        val responseString = telegramBotService.getUpdates(updateId)
        println(responseString)

        val response: Response = json.decodeFromString(responseString)
        val lastUpdate = response.result.lastOrNull() ?: continue
        updateId = lastUpdate.updateId + 1

        val chatId = lastUpdate.message?.chat?.id
            ?: lastUpdate.callbackQuery?.message?.chat?.id
            ?: continue

        val message = lastUpdate.message?.text
        val data = lastUpdate.callbackQuery?.data

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

@Serializable
data class Update(
    @SerialName("update_id")
    val updateId: Long,
    @SerialName("message")
    val message: Message? = null,
    @SerialName("callback_query")
    val callbackQuery: CallbackQuery? = null,
)

@Serializable
data class Response(
    @SerialName("result")
    val result: List<Update>,
)

@Serializable
data class Message(
    @SerialName("text")
    val text: String? = null,
    @SerialName("chat")
    val chat: Chat,
)

@Serializable
data class CallbackQuery(
    @SerialName("data")
    val data: String,
    @SerialName("message")
    val message: Message,
)

@Serializable
data class Chat(
    @SerialName("id")
    val id: Long,
)

@Serializable
data class SendMessageRequest(
    @SerialName("chat_id")
    val chatId: Long,
    @SerialName("text")
    val text: String,
    @SerialName("reply_markup")
    val replyMarkup: ReplyMarkup? = null,
)

@Serializable
data class ReplyMarkup(
    @SerialName("inline_keyboard")
    val inlineKeyboard: List<List<InlineKeyboard>>,
)

@Serializable
data class InlineKeyboard(
    @SerialName("text")
    val text: String,
    @SerialName("callback_data")
    val callbackData: String,
)