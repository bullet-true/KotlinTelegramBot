package ru.ifedorov.telegrambot.telegram.service.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeleteMessageResponse(
    @SerialName("ok")
    val ok: Boolean,
    @SerialName("result")
    val result: Boolean
)