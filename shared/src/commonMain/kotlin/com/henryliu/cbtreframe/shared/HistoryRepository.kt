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
        timestamp: Long
    ) {
        queries.insertHistory(id, inputText, aiResponse, timestamp)
    }

    fun deleteHistory(id: String) {
        queries.deleteHistoryById(id)
    }
}
