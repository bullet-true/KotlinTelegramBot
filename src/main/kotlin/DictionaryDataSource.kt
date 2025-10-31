package org.example

import java.io.File

fun main() {
    val wordsFile = File("words.txt")
    updateDictionary(wordsFile)
}

fun updateDictionary(wordsFile: File) {
    if (!wordsFile.exists()) {
        throw IllegalStateException("Файл словаря ${wordsFile.name} не найден")
    }

    val connection = DatabaseConnection.connection
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

                connection.prepareStatement(
                    """
                            INSERT INTO words (text, translate) VALUES (?, ?)
                            ON CONFLICT(text) DO UPDATE SET translate = excluded.translate;
                        """.trimIndent()
                ).use { ps ->
                    ps.setString(1, original)
                    ps.setString(2, translate)
                    ps.executeUpdate()
                }

            } catch (e: IndexOutOfBoundsException) {
                throw IllegalStateException("Некорректное содержание файла словаря $wordsFile. $e")
            }
        }
    }
}