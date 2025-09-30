package org.example

data class Question(
    val variants: List<Word>,
    val correctAnswer: Word,
)

fun Question.asConsoleString(): String {
    val variants = this.variants
        .mapIndexed { index, word -> " ${index + 1} - ${word.translate}" }
        .joinToString(separator = "\n")
    return "\n" + this.correctAnswer.original + ":" + "\n" + variants + "\n ---------- \n 0 - Меню"
}