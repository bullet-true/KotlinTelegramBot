import org.junit.jupiter.api.Test
import ru.ifedorov.telegrambot.data.FileUserDictionary
import ru.ifedorov.telegrambot.trainer.LearnWordsTrainer
import ru.ifedorov.telegrambot.trainer.model.Statistics
import java.io.File
import kotlin.test.*

class LearnWordsTrainerTest {

    @Test
    fun `test statistics with 4 words of 7`() {
        val file = this::class.java.classLoader.getResource("4_words_of_7.txt")!!.file
        val trainer = LearnWordsTrainer(dictionary = FileUserDictionary(file), 0L, "test")

        assertEquals(
            Statistics(learnedCount = 4, totalCount = 7, percent = 57),
            trainer.getStatistics()
        )
    }

    @Test
    fun `test statistics with corrupted file`() {
        val file = this::class.java.classLoader.getResource("corrupted_file.txt")!!.file

        assertFailsWith<IllegalStateException> {
            val trainer = LearnWordsTrainer(dictionary = FileUserDictionary(file), 0L, "test")
            trainer.getStatistics()
        }
    }

    @Test
    fun `test getNextQuestion() with 5 unlearned words`() {
        val file = this::class.java.classLoader.getResource("5_unlearned_words.txt")!!.file
        val trainer = LearnWordsTrainer(dictionary = FileUserDictionary(file), 0L, "test")
        val question = trainer.getNextQuestion()

        assertNotNull(question)
        assertEquals(4, question.variants.size)
        assertTrue(question.variants.contains(question.correctAnswer))
    }

    @Test
    fun `test getNextQuestion() with 1 unlearned word`() {
        val file = this::class.java.classLoader.getResource("1_unlearned_word.txt")!!.file
        val trainer = LearnWordsTrainer(dictionary = FileUserDictionary(file), 0L, "test")
        val question = trainer.getNextQuestion()

        assertNotNull(question)
        assertEquals(4, question.variants.size)
        assertTrue(question.variants.contains(question.correctAnswer))
    }

    @Test
    fun `test getNextQuestion() with all words learned`() {
        val file = this::class.java.classLoader.getResource("all_words_learned.txt")!!.file
        val trainer = LearnWordsTrainer(dictionary = FileUserDictionary(file), 0L, "test")
        val question = trainer.getNextQuestion()

        assertNull(question)
    }

    @Test
    fun `test checkAnswer() with true`() {
        val file = this::class.java.classLoader.getResource("4_words_of_7.txt")!!.file
        val trainer = LearnWordsTrainer(dictionary = FileUserDictionary(file), 0L, "test")
        val question = trainer.getNextQuestion()

        val correctIndex = question!!.variants.indexOf(question.correctAnswer)
        val result = trainer.checkAnswer(correctIndex)
        assertTrue(result)
    }

    @Test
    fun `test checkAnswer() with false`() {
        val file = this::class.java.classLoader.getResource("4_words_of_7.txt")!!.file
        val trainer = LearnWordsTrainer(dictionary = FileUserDictionary(file), 0L, "test")
        val question = trainer.getNextQuestion()

        val correctIndex = question!!.variants.indexOf(question.correctAnswer)
        val incorrectIndexList = question.variants.indices.filter { it != correctIndex }

        incorrectIndexList.forEach {
            assertFalse(trainer.checkAnswer(it))
        }
    }

    @Test
    fun `test resetProgress() with 2 words in dictionary`() {
        val file = this::class.java.classLoader.getResource("2_words_in_dictionary.txt")!!.file
        val ordinalText = File(file).readText()

        val dictionary = FileUserDictionary(file)
        val trainer = LearnWordsTrainer(dictionary, 0L, "test")
        val unlearnedWords = dictionary.getUnlearnedWords(0L, "test")
        val learnedWords = dictionary.getLearnedWords(0L, "test")

        try {
            assertTrue(unlearnedWords.any { it.correctAnswersCount > 0 })
            assertTrue(learnedWords.any { it.correctAnswersCount > 0 })
            assertEquals(2, dictionary.getSize())

            trainer.resetProgress()

            assertTrue(unlearnedWords.all { it.correctAnswersCount == 0 })
            assertTrue(learnedWords.all { it.correctAnswersCount == 0 })

        } finally {
            File(file).writeText(ordinalText)
        }
    }
}