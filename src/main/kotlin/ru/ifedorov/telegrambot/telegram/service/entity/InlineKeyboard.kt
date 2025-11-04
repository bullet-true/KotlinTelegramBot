package ru.ifedorov.telegrambot.telegram.service.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InlineKeyboard(
    @SerialName("text")
    val text: String,
    @SerialName("callback_data")
    val callbackData: String,
)