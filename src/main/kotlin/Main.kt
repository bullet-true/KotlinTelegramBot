package org.example

fun main() {

    val trainer = LearnWordsTrainer()

    while (true) {
        println(
            """
            Меню: 
            1 – Учить слова
            2 – Статистика
            0 – Выход
        """.trimIndent()
        )

        when (readln()) {
            "1" -> trainer.startLearning()
            "2" -> trainer.showStatistics()
            "0" -> break
            else -> println("Введите число 1, 2 или 0")
        }
    }
}