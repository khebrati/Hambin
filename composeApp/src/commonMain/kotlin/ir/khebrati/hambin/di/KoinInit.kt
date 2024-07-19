package ir.khebrati.hambin.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

fun initKoin(nativeModules: List<Module>) = startKoin {
    modules(
        commonModule(),
        platformModule(),
        module { includes(nativeModules) }
    )
}