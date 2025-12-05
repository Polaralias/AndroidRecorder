// Gradle 9.x Configuration
// Using buildscript for Android plugins (better compatibility with Gradle 9.x)
import org.gradle.api.GradleException

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.6.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.57.2")
    }
}

// Using plugins DSL for Kotlin plugins (natively supported by Gradle Plugin Portal)
plugins {
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.kotlin.kapt") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
}

tasks.register("enforceSingleStringDependencyNotation") {
    group = "verification"
    description = "Fails if deprecated multi-argument dependency notation is used."

    val deprecatedPattern = """\b(?:implementation|api|compileOnly|runtimeOnly|testImplementation|androidTestImplementation|classpath)\s*\(\s*['\"][^'\"]+['\"]\s*,\s*['\"][^'\"]+['\"]\s*,\s*['\"][^'\"]+['\"]\s*\)""".toRegex()
    val filesToScan = listOf("build.gradle.kts", "app/build.gradle.kts", "settings.gradle.kts")

    doLast {
        filesToScan.map(::file)
            .filter { it.exists() }
            .forEach { script ->
                if (deprecatedPattern.containsMatchIn(script.readText())) {
                    throw GradleException("Deprecated multi-argument dependency notation found in ${script.path}. Use single-string coordinates instead.")
                }
            }
    }
}
