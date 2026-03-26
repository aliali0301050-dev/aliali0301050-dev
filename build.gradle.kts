// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
}

subprojects {
    configurations.all {
        resolutionStrategy {
            cacheDynamicVersionsFor(0, "seconds")
            cacheChangingModulesFor(0, "seconds")
        }
    }
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}

println("====================================================")
println("🚀 Project: MP3 Music Player Pro")
println("💻 Status: Gradle 8.5 & Kotlin 1.9.24 - Stable")
println("🕒 Start Time: ${java.time.LocalDateTime.now()}")
println("====================================================")
