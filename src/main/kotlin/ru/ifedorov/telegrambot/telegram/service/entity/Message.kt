package ru.ifedorov.telegrambot.telegram.service.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    @SerialName("text")
    val text: String? = null,
    @SerialName("chat")
    val chat: Chat,
)