package org.example

import java.io.File

fun main() {

    val dictionary = loadDictionary()

    while (true) {
        println(
            """
            Меню: 
            1 – Учить слова
            2 – Статистика
            0 – Выход
        """.trimIndent()
        )

        val userInput = readln()
        when (userInput) {
            "1" -> println("Учить слова")
            "2" -> showStatistics(dictionary)
            "0" -> break
            else -> println("Введите число 1, 2 или 0")
        }
    }
}

fun loadDictionary(): List<Word> {
    val wordsFile = File("words.txt")
    val wordsList = wordsFile.readLines()
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
}

fun showStatistics(dictionary: List<Word>) {
    val totalCount = dictionary.size
    val learnedCount = dictionary.filter { it.correctAnswersCount >= 3 }.size
    val percent = if (totalCount > 0) (learnedCount * 100 / totalCount) else 0

    println("Выучено $learnedCount из $totalCount слов | $percent%\n")
}