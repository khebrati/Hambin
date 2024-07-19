package ir.khebrati.hambin.di

import ir.khebrati.hambin.TestClass
import org.koin.core.module.Module
import org.koin.dsl.module

fun commonModule() = module {
    factory { TestClass() }
}
expect fun platformModule() : Module