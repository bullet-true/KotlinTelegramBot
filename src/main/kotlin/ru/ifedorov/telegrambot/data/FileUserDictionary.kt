package ru.ifedorov.telegrambot.data

import ru.ifedorov.telegrambot.data.db.DEFAULT_LEARNING_THRESHOLD
import ru.ifedorov.telegrambot.trainer.IUserDictionary
import ru.ifedorov.telegrambot.trainer.model.Word
import java.io.File

const val DICTIONARY_FILE = "words.txt"

class FileUserDictionary(
    private val fileName: String = DICTIONARY_FILE,
    private val learningThreshold: Int = DEFAULT_LEARNING_THRESHOLD,
) : IUserDictionary {

    private val dictionary = loadDictionary()

    override fun getNumOfLearnedWords(chatId: Long, username: String): Int =
        dictionary.count { it.correctAnswersCount >= learningThreshold }

    override fun getSize(): Int = dictionary.size

    override fun getLearnedWords(chatId: Long, username: String): List<Word> =
        dictionary.filter { it.correctAnswersCount >= learningThreshold }

    override fun getUnlearnedWords(chatId: Long, username: String): List<Word> =
        dictionary.filter { it.correctAnswersCount < learningThreshold }

    override fun setCorrectAnswersCount(chatId: Long, username: String, word: String, correctAnswersCount: Int) {
        dictionary.find { it.original == word }?.correctAnswersCount = correctAnswersCount
        saveDictionary()
    }

    override fun resetUserProgress(chatId: Long, username: String) {
        dictionary.forEach { it.correctAnswersCount = 0 }
        saveDictionary()
    }

    private fun loadDictionary(): List<Word> {
        try {
            val wordsFile = File(fileName)
            if (!wordsFile.exists()) {
                File(DICTIONARY_FILE).copyTo(wordsFile)
            }

            val wordsList = wordsFile.readLines()
            if (wordsList.isEmpty()) {
                throw IllegalStateException("Файл словаря пустой: $fileName")
            }

            val dictionary = mutableListOf<Word>()

            wordsList.forEach {
                val line = it.split("|")
                val original = line[0]
                val translate = line[1]
                val correctAnswersCount = line.getOrNull(2)?.toIntOrNull() ?: 0
                val word = Word(original, translate, correctAnswersCount)
                dictionary.add(word)
            }
            return dictionary
        } catch (e: IndexOutOfBoundsException) {
            throw IllegalStateException("Некорректное содержание файла словаря $fileName. $e")
        }
    }

    private fun saveDictionary() {
        val wordsFile = File(fileName)
        wordsFile.writeText(dictionary.joinToString("\n") {
            "${it.original}|${it.translate}|${it.correctAnswersCount}"
        })
    }
}