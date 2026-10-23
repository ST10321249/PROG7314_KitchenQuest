package com.example.kitchenquest.data.network

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

enum class ApiErrorType {
    NO_CONNECTION,
    TIMEOUT,
    UNAUTHORIZED,
    NOT_FOUND,
    INVALID_REQUEST,
    SERVER,
    UNKNOWN
}

class ApiException(
    val type: ApiErrorType,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

fun Throwable.toApiException(): ApiException {
    return when (this) {

        is ApiException ->
            this

        is SocketTimeoutException ->
            ApiException(
                ApiErrorType.TIMEOUT,
                "The server took too long to respond. Please try again.",
                this
            )

        is IOException ->
            ApiException(
                ApiErrorType.NO_CONNECTION,
                "Can't reach the server. Check your connection and try again.",
                this
            )

        is HttpException ->
            when (code()) {

                400 ->
                    ApiException(
                        ApiErrorType.INVALID_REQUEST,
                        serverMessage()
                            ?: "Some of the information entered isn't valid.",
                        this
                    )

                401 ->
                    ApiException(
                        ApiErrorType.UNAUTHORIZED,
                        "Your session has expired. Please sign in again.",
                        this
                    )

                404 ->
                    ApiException(
                        ApiErrorType.NOT_FOUND,
                        serverMessage()
                            ?: "The requested information couldn't be found.",
                        this
                    )

                in 500..599 ->
                    ApiException(
                        ApiErrorType.SERVER,
                        "Something went wrong on our side. Please try again later.",
                        this
                    )

                else ->
                    ApiException(
                        ApiErrorType.UNKNOWN,
                        serverMessage()
                            ?: "Something went wrong. Please try again.",
                        this
                    )
            }

        else ->
            ApiException(
                ApiErrorType.UNKNOWN,
                "Something went wrong. Please try again.",
                this
            )
    }
}

private fun HttpException.serverMessage(): String? {
    return try {

        val body =
            response()
                ?.errorBody()
                ?.string()
                ?: return null

        ApiClient.json
            .parseToJsonElement(body)
            .jsonObject["error"]
            ?.jsonPrimitive
            ?.content

    } catch (error: Exception) {
        null
    }
}

// Runs an API call and turns any failure into an ApiException.
suspend fun <T> apiCall(
    block: suspend () -> T
): Result<T> {
    return try {

        Result.success(
            block()
        )

    } catch (
        error: CancellationException
    ) {

        throw error

    } catch (
        error: Exception
    ) {

        Result.failure(
            error.toApiException()
        )
    }
}