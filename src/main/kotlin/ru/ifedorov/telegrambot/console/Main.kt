package ru.ifedorov.telegrambot.console

import ru.ifedorov.telegrambot.data.db.DatabaseConnection
import ru.ifedorov.telegrambot.data.db.DatabaseUserDictionary
import ru.ifedorov.telegrambot.trainer.LearnWordsTrainer

fun main() {
    val chatId = 0L
    val userName = "from console trainer"

    Runtime.getRuntime().addShutdownHook(Thread {
        DatabaseConnection.close()
    })

    val trainer = try {
        LearnWordsTrainer(DatabaseUserDictionary(chatId, userName))
    } catch (e: Exception) {
        println("Невозможно подключиться к БД. $e")
        return
    }

    while (true) {
        println(
            """
               Меню: 
               1 – Учить слова
               2 – Статистика
               0 – Выход
            """.trimIndent()
        )

        when (readln().toIntOrNull()) {
            1 -> {
                while (true) {
                    val question = trainer.getNextQuestion()
                    if (question == null) {
                        println("Все слова в словаре выучены")
                        break
                    } else {
                        println(question.asConsoleString())

                        val userAnswerInput = readln().toIntOrNull()
                        if (userAnswerInput == 0) break

                        if (trainer.checkAnswer(userAnswerInput?.minus(1))) {
                            println("Правильно!")
                        } else {
                            println("Неправильно! ${question.correctAnswer.original} – это ${question.correctAnswer.translate}")
                        }
                    }
                }
            }

            2 -> {
                val statistics = trainer.getStatistics()
                println("Выучено ${statistics.learnedCount} из ${statistics.totalCount} слов | ${statistics.percent}%\n")
            }

            0 -> break
            else -> println("Введите число 1, 2 или 0")
        }
    }
}