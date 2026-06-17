package com.henryliu.cbtreframe.shared

import com.henryliu.cbtreframe.shared.db.AppDatabase
import com.henryliu.cbtreframe.shared.db.HistoryEntity
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val database: AppDatabase) {
    private val queries = database.appDatabaseQueries

    fun getHistory(): Flow<List<HistoryEntity>> {
        return queries.selectAllHistory().asFlow().mapToList(Dispatchers.IO)
    }

    fun addHistory(
        id: String,
        inputText: String,
        aiResponse: String?,
        timestamp: Long,
        distortion: String,
        alternative: String,
        action: String,
        isFavorite: Long,
        providerName: String,
        modelName: String,
        moodTag: String,
        therapyTemplateRaw: String,
        analysisDepthRaw: String,
        responseStyleRaw: String,
        resultExtrasJSON: String,
        followUpMessagesJSON: String
    ) {
        queries.insertHistory(
            id, inputText, aiResponse, timestamp,
            distortion, alternative, action, isFavorite,
            providerName, modelName, moodTag,
            therapyTemplateRaw, analysisDepthRaw, responseStyleRaw, resultExtrasJSON, followUpMessagesJSON
        )
    }

    fun updateFollowUpMessages(id: String, messagesJson: String) {
        queries.updateFollowUpMessages(messagesJson, id)
    }

    fun toggleFavorite(id: String, isFavorite: Long) {
        queries.updateFavorite(isFavorite, id)
    }

    fun deleteHistory(id: String) {
        queries.deleteHistoryById(id)
    }



    fun deleteAllHistory() {
        queries.transaction {
            queries.deleteAllHistory()
            queries.deleteAllMoodCheckIns()
        }
    }
}
