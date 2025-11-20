package ru.ifedorov.telegrambot.trainer

import ru.ifedorov.telegrambot.trainer.model.Question
import ru.ifedorov.telegrambot.trainer.model.Statistics

const val DEFAULT_ANSWER_OPTIONS_COUNT = 4

class LearnWordsTrainer(
    private val dictionary: IUserDictionary,
    private val chatId: Long,
    private val username: String,
    private val answerOptionsCount: Int = DEFAULT_ANSWER_OPTIONS_COUNT,
) {
    private var question: Question? = null

    fun getNextQuestion(): Question? {
        val unlearnedWords = dictionary.getUnlearnedWords(chatId, username)
        if (unlearnedWords.isEmpty()) return null

        val questionWords = if (unlearnedWords.size < answerOptionsCount) {
            val learnedWords = dictionary.getLearnedWords(chatId, username)
            unlearnedWords.shuffled()
                .take(answerOptionsCount) + learnedWords.take(answerOptionsCount - unlearnedWords.size)
        } else {
            unlearnedWords.shuffled().take(answerOptionsCount)
        }.shuffled()

        val correctAnswer = questionWords.random()

        question = Question(
            variants = questionWords,
            correctAnswer = correctAnswer,
        )
        return question
    }

    fun checkAnswer(userAnswerInput: Int?): Boolean {
        return question?.let {
            val correctAnswerIndex = it.variants.indexOf(it.correctAnswer)
            if (userAnswerInput == correctAnswerIndex) {
                dictionary.setCorrectAnswersCount(
                    chatId,
                    username,
                    it.correctAnswer.original,
                    it.correctAnswer.correctAnswersCount + 1
                )
                true
            } else {
                false
            }
        } ?: false
    }

    fun getStatistics(): Statistics {
        val totalCount = dictionary.getSize()
        val learnedCount = dictionary.getNumOfLearnedWords(chatId, username)
        val percent = if (totalCount > 0) (learnedCount * 100 / totalCount) else 0
        return Statistics(totalCount, learnedCount, percent)
    }

    fun getCurrentQuestion(): Question? = question

    fun resetProgress() {
        dictionary.resetUserProgress(chatId, username)
    }
}