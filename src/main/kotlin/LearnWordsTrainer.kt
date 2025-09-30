package org.example

import java.io.File

const val MIN_CORRECT_ANSWERS = 3
const val ANSWER_OPTIONS_COUNT = 4
const val DICTIONARY_FILE = "words.txt"

class LearnWordsTrainer {
    private val dictionary = loadDictionary()
    private var question: Question? = null

    fun startLearning() {
        while (true) {
            val question = getNextQuestion()

            if (question == null) {
                println("Все слова в словаре выучены")
                break
            }

            println(question.asConsoleString())

            when (val userAnswerInput = readln().toIntOrNull()) {
                0 -> break

                in 1..question.variants.size -> {
                    if (checkAnswer(userAnswerInput?.minus(1))) {
                        println("Правильно!")
                    } else {
                        println("Неправильно! ${question.correctAnswer.original} – это ${question.correctAnswer.translate}")
                    }
                }

                else -> println("Для ответа нужно ввести число от 0 до ${question.variants.size}")
            }
        }
    }

    fun showStatistics() {
        val stats = getStatistics()
        println("Выучено ${stats.learnedCount} из ${stats.totalCount} слов | ${stats.percent}%\n")
    }

    private fun getStatistics(): Statistics {
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

    private fun getNextQuestion(): Question? {
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

    private fun checkAnswer(userAnswerInput: Int?): Boolean {
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
}