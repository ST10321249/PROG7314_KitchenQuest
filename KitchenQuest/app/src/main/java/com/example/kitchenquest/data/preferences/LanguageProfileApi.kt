package com.example.kitchenquest.data.preferences

import com.example.kitchenquest.data.user.UserProfileDto
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.PUT

@Serializable
data class LanguageUpdateRequest(val language: String)

/** Uses the existing authenticated profile endpoint; never changes user-entered data. */
interface LanguageProfileApi {
    @PUT("api/users/me")
    suspend fun updateLanguage(@Body request: LanguageUpdateRequest): UserProfileDto
}
