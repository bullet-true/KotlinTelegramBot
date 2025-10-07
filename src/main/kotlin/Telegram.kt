package org.example

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

const val TELEGRAM_BASE_URL = "https://api.telegram.org/bot"

fun main(args: Array<String>) {

    val botToken = args[0]
    var updateId = 0
    val client: HttpClient = HttpClient.newBuilder().build()

    while (true) {
        Thread.sleep(2000)
        val updates = getUpdates(client, botToken, updateId)
        println(updates)

        val updateIdString = Regex("\"update_id\":(\\d+),")
            .findAll(updates)
            .lastOrNull()
            ?.groupValues
            ?.getOrNull(1)

        updateId = updateIdString?.toInt()?.plus(1) ?: continue

        println(updateId)

        val messageText = Regex("\"text\":\"(.+?)\"")
            .find(updates)
            ?.groupValues
            ?.getOrNull(1)

        println(messageText)
    }
}

fun getUpdates(client: HttpClient, botToken: String, updateId: Int): String {
    val url = "$TELEGRAM_BASE_URL$botToken/getUpdates?offset=$updateId"
    val request: HttpRequest = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .build()

    val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
    return response.body()
}