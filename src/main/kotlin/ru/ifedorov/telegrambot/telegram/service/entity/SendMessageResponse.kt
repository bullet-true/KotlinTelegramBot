package ru.ifedorov.telegrambot.telegram.service.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendMessageResponse(
    @SerialName("ok")
    val ok: Boolean,
    @SerialName("result")
    val result: SentMessageResult? = null,
)