package org.example

import java.io.Closeable
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

const val DATABASE_NAME = "data.db"
const val DEFAULT_LEARNING_THRESHOLD = 3

class DatabaseUserDictionary(
    private val chatId: Long,
    private val learningThreshold: Int = DEFAULT_LEARNING_THRESHOLD
) : IUserDictionary, Closeable {
    private val connection: Connection = DriverManager.getConnection("jdbc:sqlite:$DATABASE_NAME")

    init {
        connection.createStatement().use { statement ->
            statement.executeUpdate(
                """
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        chat_id INTEGER UNIQUE
                    );
                """.trimIndent()
            )

            statement.executeUpdate(
                """
                        CREATE TABLE IF NOT EXISTS user_answers (
                        user_id INTEGER,
                        word_id INTEGER,
                        correct_answer_count INTEGER,
                        updated_at TIMESTAMP,
                        FOREIGN KEY(user_id) REFERENCES users(id),
                        FOREIGN KEY(word_id) REFERENCES words(id),
                        UNIQUE(user_id, word_id)
                    );
                """.trimIndent()
            )
        }
    }

    private fun getUserId(): Int {
        connection.createStatement().use { statement ->
            statement.executeUpdate(
                """
                    INSERT INTO users (chat_id, created_at) 
                    VALUES ($chatId, CURRENT_TIMESTAMP) 
                    ON CONFLICT(chat_id) DO NOTHING
                """.trimIndent()
            )
        }

        connection.createStatement().use { statement ->
            val rs = statement.executeQuery("SELECT id FROM users WHERE chat_id = $chatId")
            if (rs.next()) return rs.getInt("id")
        }

        throw IllegalStateException("Не удалось создать пользователя")
    }

    override fun getNumOfLearnedWords(): Int {
        val userId = getUserId()
        connection.createStatement().use { statement ->
            val rs: ResultSet = statement.executeQuery(
                "SELECT COUNT(*) as count FROM user_answers WHERE user_id = $userId AND correct_answer_count >= $learningThreshold"
            )
            return if (rs.next()) rs.getInt("count") else 0
        }
    }

    override fun getSize(): Int {
        connection.createStatement().use { statement ->
            val rs: ResultSet = statement.executeQuery("SELECT COUNT(*) as count FROM words")
            return if (rs.next()) rs.getInt("count") else 0
        }
    }

    override fun getLearnedWords(): List<Word> {
        val userId = getUserId()
        val learnedWords = mutableListOf<Word>()
        connection.createStatement().use { statement ->
            val rs: ResultSet = statement.executeQuery(
                """
                    SELECT words.text, words.translate, user_answers.correct_answer_count
                    FROM words
                    JOIN user_answers ON words.id = user_answers.word_id
                    WHERE user_answers.user_id = $userId AND user_answers.correct_answer_count >= $learningThreshold                    
                """.trimIndent()
            )

            while (rs.next()) {
                learnedWords.add(
                    Word(
                        original = rs.getString("text"),
                        translate = rs.getString("translate"),
                        correctAnswersCount = rs.getInt("correct_answer_count")
                    )
                )
            }
        }
        return learnedWords
    }

    override fun getUnlearnedWords(): List<Word> {
        val userId = getUserId()
        val unlearnedWords = mutableListOf<Word>()

        connection.createStatement().use { statement ->
            val rs: ResultSet = statement.executeQuery(
                """
                    SELECT words.text, words.translate, IFNULL(user_answers.correct_answer_count, 0) as correct_answer_count
                    FROM words
                    LEFT JOIN user_answers ON words.id = user_answers.word_id AND user_answers.user_id = $userId
                    WHERE IFNULL(user_answers.correct_answer_count, 0) < $learningThreshold                    
                """.trimIndent()
            )

            while (rs.next()) {
                unlearnedWords.add(
                    Word(
                        original = rs.getString("text"),
                        translate = rs.getString("translate"),
                        correctAnswersCount = rs.getInt("correct_answer_count")
                    )
                )
            }
        }
        return unlearnedWords
    }

    override fun setCorrectAnswersCount(word: String, correctAnswersCount: Int) {
        val userId = getUserId()
        val wordId: Int

        connection.createStatement().use { statement ->
            val rs: ResultSet = statement.executeQuery("SELECT id FROM words WHERE text = '$word'")
            if (!rs.next()) return
            wordId = rs.getInt("id")
        }

        connection.createStatement().use { statement ->
            statement.executeUpdate(
                """
                    INSERT INTO user_answers (user_id, word_id, correct_answer_count, updated_at)
                    VALUES ($userId, $wordId, $correctAnswersCount, CURRENT_TIMESTAMP)
                    ON CONFLICT(user_id, word_id) DO UPDATE SET
                        correct_answer_count = $correctAnswersCount,
                        updated_at = CURRENT_TIMESTAMP
                """.trimIndent()
            )
        }
    }

    override fun resetUserProgress() {
        val userId = getUserId()

        connection.createStatement().use { statement ->
            statement.executeUpdate(
                "UPDATE user_answers SET correct_answer_count = 0, updated_at = CURRENT_TIMESTAMP WHERE user_id = $userId"
            )
        }
    }

    override fun close() {
        connection.close()
    }
}