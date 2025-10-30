package org.example

fun main() {
    val chatId = 0L

    try {
        DatabaseUserDictionary(chatId).use { dictionary ->
            val trainer = LearnWordsTrainer(dictionary)

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
    } catch (e: Exception) {
        println("Невозможно подключиться к БД. $e")
    }
}

fun Question.asConsoleString(): String {
    val variants = this.variants
        .mapIndexed { index, word -> " ${index + 1} - ${word.translate}" }
        .joinToString(separator = "\n")
    return "\n" + this.correctAnswer.original + ":" + "\n" + variants + "\n ---------- \n 0 - Меню"
}