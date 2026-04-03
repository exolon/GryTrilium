package com.example.grytrilium

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

data class Attribute(
    val type: String,
    val name: String,
    val value: String
)

data class NoteItem(
    val noteId: String,
    val title: String,
    val type: String = "text",
    val parentNoteIds: List<String> = emptyList(),
    val attributes: List<Attribute> = emptyList()
)

data class SearchResponse(val results: List<NoteItem>)

// Auto-Save Data Models
data class UpdateNoteRequest(val title: String)
data class CreateNoteRequest(val parentNoteId: String, val title: String, val type: String = "text", val content: String = "")

interface TriliumApi {
    @GET("etapi/notes")
    suspend fun searchNotes(
        @Header("Authorization") token: String,
        @Query("search") search: String,
        @Query("ancestorNoteId") ancestorNoteId: String? = null,
        @Query("ancestorDepth") ancestorDepth: String? = null
    ): SearchResponse

    @GET("etapi/notes/{noteId}/content")
    suspend fun getNoteContent(
        @Header("Authorization") token: String,
        @Path("noteId") noteId: String
    ): ResponseBody

    // --- AUTO-SAVE ENDPOINTS ---

    @PATCH("etapi/notes/{noteId}")
    suspend fun updateNoteMetadata(
        @Header("Authorization") token: String,
        @Path("noteId") noteId: String,
        @Body request: UpdateNoteRequest
    ): ResponseBody

    @PUT("etapi/notes/{noteId}/content")
    suspend fun updateNoteContent(
        @Header("Authorization") token: String,
        @Path("noteId") noteId: String,
        @Body content: RequestBody
    ): ResponseBody

    @POST("etapi/create-note")
    suspend fun createNote(
        @Header("Authorization") token: String,
        @Body request: CreateNoteRequest
    ): NoteItem
}