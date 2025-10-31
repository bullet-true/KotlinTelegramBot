package org.example

import java.sql.Connection
import java.sql.ResultSet

const val DATABASE_NAME = "data.db"
const val DEFAULT_LEARNING_THRESHOLD = 3

class DatabaseUserDictionary(
    private val chatId: Long,
    private val learningThreshold: Int = DEFAULT_LEARNING_THRESHOLD
) : IUserDictionary {

    private val connection: Connection = DatabaseConnection.connection

    private fun getUserId(): Int {
        connection.prepareStatement(
            """
                    INSERT INTO users (chat_id, created_at) 
                    VALUES (?, CURRENT_TIMESTAMP) 
                    ON CONFLICT(chat_id) DO NOTHING
                """.trimIndent()
        ).use { ps ->
            ps.setLong(1, chatId)
            ps.executeUpdate()
        }

        connection.prepareStatement("SELECT id FROM users WHERE chat_id = ?").use { ps ->
            ps.setLong(1, chatId)
            val rs: ResultSet = ps.executeQuery()
            if (rs.next()) return rs.getInt("id")
        }

        throw IllegalStateException("Не удалось создать пользователя")
    }

    override fun getNumOfLearnedWords(): Int {
        val userId = getUserId()

        connection.prepareStatement(
            "SELECT COUNT(*) as count FROM user_answers WHERE user_id = ? AND correct_answer_count >= ?"
        ).use { ps ->
            ps.setInt(1, userId)
            ps.setInt(2, learningThreshold)
            val rs: ResultSet = ps.executeQuery()
            return if (rs.next()) rs.getInt("count") else 0
        }
    }

    override fun getSize(): Int {
        connection.prepareStatement("SELECT COUNT(*) as count FROM words").use { ps ->
            val rs: ResultSet = ps.executeQuery()
            return if (rs.next()) rs.getInt("count") else 0
        }
    }

    override fun getLearnedWords(): List<Word> {
        val userId = getUserId()
        val learnedWords = mutableListOf<Word>()

        connection.prepareStatement(
            """
                SELECT words.text, words.translate, user_answers.correct_answer_count
                FROM words
                JOIN user_answers ON words.id = user_answers.word_id
                WHERE user_answers.user_id = ? AND user_answers.correct_answer_count >= ?                  
                """.trimIndent()
        ).use { ps ->
            ps.setInt(1, userId)
            ps.setInt(2, learningThreshold)
            val rs: ResultSet = ps.executeQuery()

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

        connection.prepareStatement(
            """
                SELECT words.text, words.translate, IFNULL(user_answers.correct_answer_count, 0) as correct_answer_count
                FROM words
                LEFT JOIN user_answers ON words.id = user_answers.word_id AND user_answers.user_id = ?
                WHERE IFNULL(user_answers.correct_answer_count, 0) < ?
                """.trimIndent()
        ).use { ps ->
            ps.setInt(1, userId)
            ps.setInt(2, learningThreshold)
            val rs: ResultSet = ps.executeQuery()

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

        connection.prepareStatement("SELECT id FROM words WHERE text = ?").use { ps ->
            ps.setString(1, word)
            val rs: ResultSet = ps.executeQuery()
            if (!rs.next()) return
            wordId = rs.getInt("id")
        }

        connection.prepareStatement(
            """
                    INSERT INTO user_answers (user_id, word_id, correct_answer_count, updated_at)
                    VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                    ON CONFLICT(user_id, word_id) DO UPDATE SET
                        correct_answer_count = ?,
                        updated_at = CURRENT_TIMESTAMP
                """.trimIndent()
        ).use { ps ->
            ps.setInt(1, userId)
            ps.setInt(2, wordId)
            ps.setInt(3, correctAnswersCount)
            ps.setInt(4, correctAnswersCount)
            ps.executeUpdate()
        }
    }

    override fun resetUserProgress() {
        val userId = getUserId()

        connection.prepareStatement(
            "UPDATE user_answers SET correct_answer_count = 0, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?"
        ).use { ps ->
            ps.setInt(1, userId)
            ps.executeUpdate()
        }
    }
}