package org.example

import java.io.File

fun main() {

    val wordsFile = File("words.txt")
    val dictionary = mutableListOf<Word>()

    val wordsList = wordsFile.readLines()
    wordsList.forEach {
        val line = it.split("|")

        val original = line[0]
        val translate = line[1]
        val correctAnswersCount = line.getOrNull(2)?.toIntOrNull() ?: 0

        val word = Word(original, translate, correctAnswersCount)
        dictionary.add(word)
    }

    println(dictionary)
}