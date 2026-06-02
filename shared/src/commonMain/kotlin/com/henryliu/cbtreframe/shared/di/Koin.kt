package com.henryliu.cbtreframe.shared.di

import com.henryliu.cbtreframe.shared.AIService
import com.henryliu.cbtreframe.shared.AIServiceImpl
import com.henryliu.cbtreframe.shared.HistoryRepository
import com.henryliu.cbtreframe.shared.HistoryViewModel
import com.henryliu.cbtreframe.shared.ReframeViewModel
import com.henryliu.cbtreframe.shared.SettingsManager
import com.henryliu.cbtreframe.shared.db.AppDatabase
import io.ktor.client.HttpClient
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(
            commonModule(),
            platformModule()
        )
    }

fun commonModule() = module {
    single { HttpClient() }
    single<AIService> { AIServiceImpl(get()) }
    single { HistoryRepository(get()) }
    single { SettingsManager(get()) }
    factory { ReframeViewModel(get(), get()) }
    factory { HistoryViewModel(get()) }
}

expect fun platformModule(): Module
