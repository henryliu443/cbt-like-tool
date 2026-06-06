package com.henryliu.cbtreframe.shared

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.henryliu.cbtreframe.shared.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ThoughtJournalRepository(private val database: AppDatabase) {
    private val queries = database.appDatabaseQueries

    fun getThoughts(): Flow<List<ThoughtEntry>> {
        return queries.selectAllThoughts().asFlow().mapToList(Dispatchers.IO).map { list ->
            list.map { entity ->
                ThoughtEntry(
                    id = entity.id,
                    content = entity.content,
                    situation = entity.situation,
                    emotion = entity.emotion,
                    intensity = entity.intensity.toInt(),
                    beliefBefore = entity.beliefBefore.toInt(),
                    beliefAfter = entity.beliefAfter.toInt(),
                    evidenceFor = entity.evidenceFor,
                    evidenceAgainst = entity.evidenceAgainst,
                    balancedThought = entity.balancedThought,
                    distortionTag = entity.distortionTag,
                    isProcessed = entity.isProcessed != 0L,
                    createdAt = entity.createdAt
                )
            }
        }
    }

    fun addThought(entry: ThoughtEntry) {
        queries.insertThought(
            id = entry.id,
            content = entry.content,
            situation = entry.situation,
            emotion = entry.emotion,
            intensity = entry.intensity.toLong(),
            beliefBefore = entry.beliefBefore.toLong(),
            beliefAfter = entry.beliefAfter.toLong(),
            evidenceFor = entry.evidenceFor,
            evidenceAgainst = entry.evidenceAgainst,
            balancedThought = entry.balancedThought,
            distortionTag = entry.distortionTag,
            isProcessed = if (entry.isProcessed) 1L else 0L,
            createdAt = entry.createdAt
        )
    }

    fun updateThought(entry: ThoughtEntry) {
        queries.updateThought(
            content = entry.content,
            situation = entry.situation,
            emotion = entry.emotion,
            intensity = entry.intensity.toLong(),
            beliefBefore = entry.beliefBefore.toLong(),
            beliefAfter = entry.beliefAfter.toLong(),
            evidenceFor = entry.evidenceFor,
            evidenceAgainst = entry.evidenceAgainst,
            balancedThought = entry.balancedThought,
            distortionTag = entry.distortionTag,
            isProcessed = if (entry.isProcessed) 1L else 0L,
            id = entry.id
        )
    }

    fun deleteThought(id: String) {
        queries.deleteThoughtById(id)
    }
}
