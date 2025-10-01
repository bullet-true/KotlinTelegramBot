package org.example

import java.io.File

const val MIN_CORRECT_ANSWERS = 3
const val ANSWER_OPTIONS_COUNT = 4
const val DICTIONARY_FILE = "words.txt"

class LearnWordsTrainer {
    private var question: Question? = null
    private val dictionary = loadDictionary()

    fun getNextQuestion(): Question? {
        val notLearnedList = dictionary.filter { it.correctAnswersCount < MIN_CORRECT_ANSWERS }
        if (notLearnedList.isEmpty()) return null

        val questionWords = notLearnedList.shuffled().take(ANSWER_OPTIONS_COUNT)
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
        val learnedCount = dictionary.count { it.correctAnswersCount >= MIN_CORRECT_ANSWERS }
        val percent = if (totalCount > 0) (learnedCount * 100 / totalCount) else 0
        return Statistics(totalCount, learnedCount, percent)
    }

    private fun loadDictionary(): List<Word> {
        val wordsFile = File(DICTIONARY_FILE)
        val wordsList = wordsFile.readLines()
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
    }

    private fun saveDictionary(dictionary: List<Word>) {
        val wordsFile = File(DICTIONARY_FILE)
        wordsFile.writeText(dictionary.joinToString("\n") {
            "${it.original}|${it.translate}|${it.correctAnswersCount}"
        })
    }
}