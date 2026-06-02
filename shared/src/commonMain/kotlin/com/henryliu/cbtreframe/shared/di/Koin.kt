package com.henryliu.cbtreframe.shared.di

import com.henryliu.cbtreframe.shared.AIService
import com.henryliu.cbtreframe.shared.AIServiceImpl
import com.henryliu.cbtreframe.shared.HistoryRepository
import com.henryliu.cbtreframe.shared.viewmodels.HistoryViewModel
import com.henryliu.cbtreframe.shared.viewmodels.MoodInsightsViewModel

import com.henryliu.cbtreframe.shared.ReframeViewModel
import com.henryliu.cbtreframe.shared.SettingsManager
import com.henryliu.cbtreframe.shared.ReframeUseCase
import com.henryliu.cbtreframe.shared.ReframeOrchestrator
import com.henryliu.cbtreframe.shared.StreakService
import com.henryliu.cbtreframe.shared.KeychainProvider
import com.henryliu.cbtreframe.shared.ThoughtJournalViewModel
import com.henryliu.cbtreframe.shared.SettingsViewModel
import com.henryliu.cbtreframe.shared.ModelFetcher
import com.henryliu.cbtreframe.shared.DefaultModelFetcher
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
    single<AIService> { AIServiceImpl(get(), get()) }
    single { HistoryRepository(get()) }
    single { SettingsManager(get()) }
    single { StreakService(get()) }
    single<ModelFetcher> { DefaultModelFetcher() }
    single { ReframeUseCase(ReframeOrchestrator, get(), get(), { providerName -> get<KeychainProvider>().load(providerName) }) }
    factory { ReframeViewModel(get(), get(), get()) }
    factory { HistoryViewModel(get()) }
    factory { MoodInsightsViewModel(get()) }

    factory { ThoughtJournalViewModel(get()) }
    factory { SettingsViewModel(get(), get(), get(), get(), get()) }
}

expect fun platformModule(): Module
