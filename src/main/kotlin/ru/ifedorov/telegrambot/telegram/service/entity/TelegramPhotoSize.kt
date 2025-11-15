package ru.ifedorov.telegrambot.telegram.service.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TelegramPhotoSize(
    @SerialName("file_id")
    val fileId: String,
)