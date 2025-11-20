package ru.ifedorov.telegrambot.telegram.service.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EditMessageResponse(
    @SerialName("ok")
    val ok: Boolean,
)