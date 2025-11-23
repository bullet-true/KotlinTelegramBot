package ru.ifedorov.telegrambot.telegram.service

class DynamicPhoto {
    private val lastPhotoMessage = HashMap<Long, Long>()

    fun savePhotoMessageId(chatId: Long, messageId: Long) {
        lastPhotoMessage[chatId] = messageId
    }

    fun getPhotoMessageId(chatId: Long): Long? = lastPhotoMessage[chatId]

    fun removePhotoMessageId(chatId: Long) {
        lastPhotoMessage.remove(chatId)
    }
}