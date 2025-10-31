package org.example

import java.sql.Connection
import java.sql.DriverManager

object DatabaseConnection {
    val connection: Connection = DriverManager.getConnection("jdbc:sqlite:$DATABASE_NAME").apply {
        createStatement().use { statement ->
            statement.execute("PRAGMA journal_mode=WAL;")
            statement.execute("PRAGMA foreign_keys=ON;")
            statement.execute("PRAGMA busy_timeout=10000;")
        }
    }

    init {
        connection.createStatement().use { statement ->
            statement.executeUpdate(
                """
                    CREATE TABLE IF NOT EXISTS words (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        text TEXT UNIQUE,
                        translate TEXT
                    );
                """.trimIndent()
            )

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

    fun close() {
        if (!connection.isClosed) {
            connection.close()
            println("Connection is closed: ${connection.isClosed}")
        }
    }
}