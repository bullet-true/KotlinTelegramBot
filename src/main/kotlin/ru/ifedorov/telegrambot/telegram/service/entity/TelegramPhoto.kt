package ru.ifedorov.telegrambot.telegram.service.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TelegramPhoto(
    @SerialName("photo")
    val photo: List<TelegramPhotoSize>? = null
)