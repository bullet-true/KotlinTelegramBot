import org.junit.jupiter.api.Test
import ru.ifedorov.telegrambot.console.asConsoleString
import ru.ifedorov.telegrambot.trainer.model.Question
import ru.ifedorov.telegrambot.trainer.model.Word
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuestionExtensionFunctionTest {
    val word1 = Word("hello", "привет", 0)
    val word2 = Word("dog", "собака", 0)
    val word3 = Word("cat", "кошка", 0)
    val word4 = Word("thank you", "спасибо", 0)

    @Test
    fun testWithFourVariants() {
        val question = Question(listOf(word1, word2, word3, word4), word1)
        val result = question.asConsoleString()
        val expected = """
             |
             | hello:
             | 1 - привет
             | 2 - собака
             | 3 - кошка
             | 4 - спасибо
             | ---------- 
             | 0 - Меню
        """.trimMargin()

        assertEquals(expected, result)
    }

    @Test
    fun testWithChangedOrderOfVariants() {
        val question = Question(listOf(word4, word3, word2, word1), word1)
        val result = question.asConsoleString()
        val expected = """
             |
             | hello:
             | 1 - спасибо
             | 2 - кошка
             | 3 - собака
             | 4 - привет
             | ---------- 
             | 0 - Меню
        """.trimMargin()

        assertEquals(expected, result)
    }

    @Test
    fun testWithEmptyListOfVariants() {
        val question = Question(emptyList(), word1)
        val result = question.asConsoleString()
        val expected = """
             |
             | hello:
             |
             | ---------- 
             | 0 - Меню
        """.trimMargin()

        assertEquals(expected, result)
    }

    @Test
    fun testWithTenWords() {
        val question = Question(
            variants = (1..10).map { Word("word-$it", "translate-$it") },
            correctAnswer = Word("word-5", "translate-5")
        )
        val result = question.asConsoleString()
        val expected = """
             |
             | word-5:
             | 1 - translate-1
             | 2 - translate-2
             | 3 - translate-3
             | 4 - translate-4
             | 5 - translate-5
             | 6 - translate-6
             | 7 - translate-7
             | 8 - translate-8
             | 9 - translate-9
             | 10 - translate-10
             | ---------- 
             | 0 - Меню
        """.trimMargin()

        assertEquals(expected, result)
    }

    @Test
    fun testWithTwoHundredWords() {
        val question = Question(
            variants = (1..200).map { Word("word-$it", "translate-$it") },
            correctAnswer = Word("word-5", "translate-5")
        )
        val result = question.asConsoleString()
        val lines = result.lines()

        assertEquals(lines[1], " ${question.correctAnswer.original}:")
        assertEquals(4 + question.variants.size, lines.size)
        assertTrue(lines.contains(" 1 - translate-1"))
        assertTrue(lines.contains(" 100 - translate-100"))
        assertTrue(lines.contains(" 200 - translate-200"))
    }

    @Test
    fun testWithSpecialSymbols() {
        val word1 = Word("{}[]()<>", "{}[]()<>")
        val word2 = Word(".,:;!?", ".,:;!?")
        val word3 = Word("|\\/", "|\\/")
        val word4 = Word("~@#$%^&*", "~@#$%^&*")
        val word5 = Word("-_\"'", "-_\"'")

        val question = Question(listOf(word1, word2, word3, word4, word5), word1)
        val result = question.asConsoleString()
        val expected = """
             |
             | {}[]()<>:
             | 1 - {}[]()<>
             | 2 - .,:;!?
             | 3 - |\/
             | 4 - ~@#$%^&*
             | 5 - -_"'
             | ---------- 
             | 0 - Меню
        """.trimMargin()

        assertEquals(expected, result)
    }

    @Test
    fun testWithSpacesWords() {
        val question = Question(
            variants = listOf(Word(" ", " "), Word("  ", "  ")),
            correctAnswer = Word(" ", " ")
        )
        val result = question.asConsoleString()
        val lines = result.lines()

        assertTrue(result.contains(" 1 -  "))
        assertTrue(result.contains(" 2 -   "))
        assertEquals(lines[1], " ${question.correctAnswer.original}:")
        assertEquals(4 + question.variants.size, lines.size)
    }

    @Test
    fun testWithLongWords() {
        val question = Question(
            listOf(
                Word(
                    "It's a first long long long long long long long long long long long long word contains original",
                    "It's a first long long long long long long long long long long long long word contains translate"
                ),
                Word(
                    "It's a second long long long long long long long long long long long long word contains original",
                    "It's a second long long long long long long long long long long long long word contains translate"
                )
            ),
            Word(
                "It's a long long long long long long long long long long long long long long correct answer ordinal",
                "It's a long long long long long long long long long long long long long long correct answer translate"
            )
        )
        val result = question.asConsoleString()
        val expected = """
             |
             | It's a long long long long long long long long long long long long long long correct answer ordinal:
             | 1 - It's a first long long long long long long long long long long long long word contains translate
             | 2 - It's a second long long long long long long long long long long long long word contains translate
             | ---------- 
             | 0 - Меню
        """.trimMargin()

        assertEquals(expected, result)
    }
}