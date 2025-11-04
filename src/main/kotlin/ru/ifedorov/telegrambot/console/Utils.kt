package ru.ifedorov.telegrambot.console

import ru.ifedorov.telegrambot.trainer.model.Question

fun Question.asConsoleString(): String =
    this.variants
        .mapIndexed { index, word -> " ${index + 1} - ${word.translate}" }
        .joinToString(
            prefix = "\n ${correctAnswer.original}:\n",
            separator = "\n",
            postfix = "\n ---------- \n 0 - Меню"
        )