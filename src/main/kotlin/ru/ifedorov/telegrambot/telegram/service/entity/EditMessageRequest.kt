package ru.ifedorov.telegrambot.telegram.service.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EditMessageRequest(
    @SerialName("chat_id")
    val chatId: Long,
    @SerialName("message_id")
    val messageId: Long,
    @SerialName("text")
    val text: String,
    @SerialName("parse_mode")
    val parseMode: String? = null,
    @SerialName("reply_markup")
    val replyMarkup: ReplyMarkup? = null,
)