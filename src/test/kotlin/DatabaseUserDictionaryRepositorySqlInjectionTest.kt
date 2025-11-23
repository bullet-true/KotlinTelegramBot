import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import ru.ifedorov.telegrambot.data.db.DatabaseUserDictionaryRepository

class DatabaseUserDictionaryRepositorySqlInjectionTest {
    private val repo = DatabaseUserDictionaryRepository()

    private val maliciousInputs = listOf(
        "'; DROP TABLE users; --",
        "admin' OR '1'='1",
        "' UNION SELECT * FROM users --",
        "test'; DELETE FROM users WHERE 1=1; --",
        "\" OR \"1\"=\"1",
    )

    private val safeInputs = listOf(
        "test",
        "testTest123",
        "hello_word",
        "hello   world",
        "\\file-path_01.txt",
        "username@example.com",
        "UNION",
        ":@+=,",
        "DROP",
        "DELETE"
    )

    @Test
    fun `setCorrectAnswersCount test SQL injection`() {
        maliciousInputs.forEach { word ->
            assertThrows<IllegalArgumentException> {
                repo.setCorrectAnswersCount(
                    chatId = 1,
                    username = "test",
                    word = word,
                    correctAnswersCount = 1
                )
            }
        }
    }

    @Test
    fun `saveImagePathForWord test SQL injection`() {
        maliciousInputs.forEach { word ->
            assertThrows<IllegalArgumentException> {
                repo.saveImagePathForWord(
                    wordId = 1,
                    localPath = word
                )
            }
        }
    }

    @Test
    fun `saveTelegramFileIdForWord test SQL injection`() {
        maliciousInputs.forEach { word ->
            assertThrows<IllegalArgumentException> {
                repo.saveTelegramFileIdForWord(
                    wordId = 1,
                    fileId = word
                )
            }
        }
    }

    @Test
    fun `setCorrectAnswersCount test safe strings`() {
        safeInputs.forEach { word ->
            assertDoesNotThrow {
                repo.setCorrectAnswersCount(
                    chatId = 1,
                    username = "test",
                    word = word,
                    correctAnswersCount = 1
                )
            }
        }
    }

    @Test
    fun `saveImagePathForWord test safe strings`() {
        safeInputs.forEach { word ->
            assertDoesNotThrow {
                repo.saveImagePathForWord(
                    wordId = 1,
                    localPath = word
                )
            }
        }
    }

    @Test
    fun `saveTelegramFileIdForWord test safe strings`() {
        safeInputs.forEach { word ->
            assertDoesNotThrow {
                repo.saveTelegramFileIdForWord(
                    wordId = 1,
                    fileId = word
                )
            }
        }
    }
}