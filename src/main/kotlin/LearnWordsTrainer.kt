package org.example

import java.io.File

const val DEFAULT_LEARNED_ANSWER_COUNT = 3
const val DEFAULT_COUNT_OF_QUESTION_WORDS = 4
const val DICTIONARY_FILE = "words.txt"

class LearnWordsTrainer(
    private val learnedAnswerCount: Int = DEFAULT_LEARNED_ANSWER_COUNT,
    private val countOfQuestionWords: Int = DEFAULT_COUNT_OF_QUESTION_WORDS,
) {
    private var question: Question? = null
    private val dictionary = loadDictionary()

    fun getNextQuestion(): Question? {
        val notLearnedList = dictionary.filter { it.correctAnswersCount < learnedAnswerCount }
        if (notLearnedList.isEmpty()) return null

        val questionWords = if (notLearnedList.size < countOfQuestionWords) {
            val learnedList = dictionary.filter { it.correctAnswersCount >= learnedAnswerCount }.shuffled()
            notLearnedList.shuffled()
                .take(countOfQuestionWords) + learnedList.take(countOfQuestionWords - notLearnedList.size)
        } else {
            notLearnedList.shuffled().take(countOfQuestionWords)
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
                it.correctAnswer.correctAnswersCount++
                saveDictionary(dictionary)
                true
            } else {
                false
            }
        } ?: false
    }

    fun getStatistics(): Statistics {
        val totalCount = dictionary.size
        val learnedCount = dictionary.count { it.correctAnswersCount >= learnedAnswerCount }
        val percent = if (totalCount > 0) (learnedCount * 100 / totalCount) else 0
        return Statistics(totalCount, learnedCount, percent)
    }

    private fun loadDictionary(): List<Word> {
        try {
            val wordsFile = File(DICTIONARY_FILE)
            if (!wordsFile.exists()) {
                throw IllegalStateException("Файл словаря не найден: $DICTIONARY_FILE")
            }

            val wordsList = wordsFile.readLines()
            if (wordsList.isEmpty()) {
                throw IllegalStateException("Файл словаря пустой: $DICTIONARY_FILE")
            }

            val dictionary = mutableListOf<Word>()

            wordsList.forEach {
                val line = it.split("|")
                val original = line[0]
                val translate = line[1]
                val correctAnswersCount = line.getOrNull(2)?.toIntOrNull() ?: 0
                val word = Word(original, translate, correctAnswersCount)
                dictionary.add(word)
            }
            return dictionary
        } catch (e: IndexOutOfBoundsException) {
            throw IllegalStateException("Некорректное содержание файла словаря $DICTIONARY_FILE. $e")
        }
    }

    private fun saveDictionary(dictionary: List<Word>) {
        val wordsFile = File(DICTIONARY_FILE)
        wordsFile.writeText(dictionary.joinToString("\n") {
            "${it.original}|${it.translate}|${it.correctAnswersCount}"
        })
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