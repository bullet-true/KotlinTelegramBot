package ru.ifedorov.telegrambot.trainer.model

data class Question(
    val variants: List<Word>,
    val correctAnswer: Word,
)