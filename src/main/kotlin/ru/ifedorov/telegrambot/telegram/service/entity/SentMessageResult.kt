package ru.ifedorov.telegrambot.telegram.service.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SentMessageResult(
    @SerialName("message_id")
    val messageId: Long
)