package ru.ifedorov.telegrambot.data.db

import java.sql.Connection
import java.sql.DriverManager

const val DATABASE_NAME = "data.db"

object DatabaseConnection {
    val connection: Connection = try {
        DriverManager.getConnection("jdbc:sqlite:${DATABASE_NAME}").apply {
            createStatement().use { statement ->
                statement.execute("PRAGMA journal_mode=WAL;")
                statement.execute("PRAGMA foreign_keys=ON;")
                statement.execute("PRAGMA busy_timeout=10000;")
            }
        }
    } catch (e: Exception) {
        println("Не удалось подключиться к БД: ${e.message}")
        throw IllegalStateException("БД недоступна. $e")
    }

    fun close() {
        if (!connection.isClosed) {
            connection.close()
            println("Connection is closed: ${connection.isClosed}")
        }
    }
}