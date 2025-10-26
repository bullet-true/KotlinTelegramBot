package org.example

import java.io.File
import java.sql.DriverManager

fun main() {
    DriverManager.getConnection("jdbc:sqlite:data.db").use { connection ->
        connection.createStatement().use { statement ->
            statement.executeUpdate(
                """
                        CREATE TABLE IF NOT EXISTS words (
                          id INTEGER PRIMARY KEY AUTOINCREMENT,
                          text TEXT UNIQUE,
                          translate TEXT
                        );
                    """.trimIndent()
            )
        }
    }

    val wordsFile = File("words.txt")
    updateDictionary(wordsFile)
}

fun updateDictionary(wordsFile: File) {
    if (!wordsFile.exists()) {
        throw IllegalStateException("Файл словаря ${wordsFile.name} не найден")
    }

    DriverManager.getConnection("jdbc:sqlite:data.db").use { connection ->
        connection.createStatement().use { statement ->
            statement.executeUpdate(
                """
                    CREATE TABLE IF NOT EXISTS words (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        text TEXT UNIQUE,
                        translate TEXT
                    );
                """.trimIndent()
            )

            val wordsList = wordsFile.readLines()
            if (wordsList.isEmpty()) {
                throw IllegalStateException("Файл словаря пустой: ${wordsFile.absolutePath}")
            }

            wordsList.forEach {
                try {
                    val line = it.split("|")
                    val original = line[0].trim()
                    val translate = line[1].trim()
                    statement.executeUpdate(
                        """
                        INSERT INTO words (text, translate) VALUES ('$original', '$translate')
                        ON CONFLICT(text) DO UPDATE SET translate = excluded.translate;
                        """.trimIndent()
                    )

                } catch (e: IndexOutOfBoundsException) {
                    throw IllegalStateException("Некорректное содержание файла словаря $wordsFile. $e")
                }
            }
        }
    }
}