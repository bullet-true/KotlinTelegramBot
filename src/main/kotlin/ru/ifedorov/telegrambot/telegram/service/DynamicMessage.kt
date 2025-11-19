package ru.ifedorov.telegrambot.telegram.service

class DynamicMessage {
    private val lastMessage = HashMap<Long, Long>()

    fun saveMessageId(chatId: Long, messageId: Long) {
        lastMessage[chatId] = messageId
    }

    fun getMessageId(chatId: Long) = lastMessage[chatId]

    fun removeMessageId(chatId: Long) {
        lastMessage.remove(chatId)
    }
}