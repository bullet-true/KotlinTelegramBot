package org.example

const val DEFAULT_ANSWER_OPTIONS_COUNT = 4

class LearnWordsTrainer(
    private val dictionary: IUserDictionary,
    private val answerOptionsCount: Int = DEFAULT_ANSWER_OPTIONS_COUNT,
) {
    private var question: Question? = null


    fun getNextQuestion(): Question? {
        val unlearnedWords = dictionary.getUnlearnedWords()
        if (unlearnedWords.isEmpty()) return null

        val questionWords = if (unlearnedWords.size < answerOptionsCount) {
            val learnedWords = dictionary.getLearnedWords()
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
                dictionary.setCorrectAnswersCount(it.correctAnswer.original, it.correctAnswer.correctAnswersCount + 1)
                true
            } else {
                false
            }
        } ?: false
    }

    fun getStatistics(): Statistics {
        val totalCount = dictionary.getSize()
        val learnedCount = dictionary.getNumOfLearnedWords()
        val percent = if (totalCount > 0) (learnedCount * 100 / totalCount) else 0
        return Statistics(totalCount, learnedCount, percent)
    }

    fun getCurrentQuestion(): Question? = question

    fun resetProgress() {
        dictionary.resetUserProgress()
    }
}

data class Word(
    val original: String,
    val translate: String,
    var correctAnswersCount: Int = 0,
)

data class Question(
    val variants: List<Word>,
    val correctAnswer: Word,
)

data class Statistics(
    val totalCount: Int,
    val learnedCount: Int,
    val percent: Int,
)