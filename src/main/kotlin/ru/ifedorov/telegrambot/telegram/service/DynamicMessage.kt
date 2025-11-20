package ru.ifedorov.telegrambot.telegram.service

class DynamicMessage {
    private val lastMessage = HashMap<Long, Long>()
    private val statisticsMessageId = HashMap<Long, Long>()
    private val statisticsTextHistory = HashMap<Long, MutableList<String>>()

    fun saveMessageId(chatId: Long, messageId: Long) {
        lastMessage[chatId] = messageId
    }

    fun getMessageId(chatId: Long) = lastMessage[chatId]

    fun removeMessageId(chatId: Long) {
        lastMessage.remove(chatId)
    }

    fun saveStatisticsMessageId(chatId: Long, messageId: Long) {
        statisticsMessageId[chatId] = messageId
    }

    fun getStatisticsMessageId(chatId: Long): Long? = statisticsMessageId[chatId]

    fun saveStatisticsText(chatId: Long, text: String) {
        val history = statisticsTextHistory.getOrPut(chatId) { mutableListOf() }
        if (history.lastOrNull() != text) {
            history.add(text)
        }
    }

    fun undoStatisticsText(chatId: Long): String? {
        val history = statisticsTextHistory[chatId] ?: return null
        if (history.size > 1) {
            history.removeLast()
            return history.lastOrNull()
        }
        return history.lastOrNull()
    }
}