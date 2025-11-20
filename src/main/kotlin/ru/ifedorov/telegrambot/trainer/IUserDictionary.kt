package ru.ifedorov.telegrambot.trainer

import ru.ifedorov.telegrambot.trainer.model.Word

interface IUserDictionary {
    fun getNumOfLearnedWords(chatId: Long, username: String): Int
    fun getSize(): Int
    fun getLearnedWords(chatId: Long, username: String): List<Word>
    fun getUnlearnedWords(chatId: Long, username: String): List<Word>
    fun setCorrectAnswersCount(chatId: Long, username: String, word: String, correctAnswersCount: Int)
    fun resetUserProgress(chatId: Long, username: String)
}