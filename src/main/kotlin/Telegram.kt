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

        val startUpdateIdIndex = updates.lastIndexOf("update_id")
        val endUpdateIdIndex = updates.lastIndexOf(",\n\"message")

        if (startUpdateIdIndex == -1 || endUpdateIdIndex == -1) continue

        val updateIdString = updates.substring(startUpdateIdIndex + 11, endUpdateIdIndex)
        updateId = updateIdString.toInt() + 1
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