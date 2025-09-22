package org.example

import java.io.File

fun main() {

    val wordsFile = File("words.txt")

    val wordsList = wordsFile.readLines()
    wordsList.forEach {
        println(it)
    }
}