import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

// App variant that controls which modules are compiled into the build.
// Supported values: "full" | "coaching" | "rating"
val appVariant = (project.findProperty("appVariant") as? String)?.lowercase() ?: "full"
require(appVariant in setOf("full", "coaching", "rating")) {
    "Invalid appVariant '$appVariant'. Allowed: full, coaching, rating"
}

val generateAppVariant = tasks.register("generateAppVariant") {
    group = "build"
    description = "Generates AppVariant.kt for the active appVariant"
    val outputDir = layout.buildDirectory.dir("generated/appVariant/kotlin")
    inputs.property("appVariant", appVariant)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().asFile.resolve("de/exhumedo/kmp/handball_support/config/AppVariant.kt")
        file.parentFile.mkdirs()
        val values = when (inputs.properties["appVariant"]) {
            "coaching" -> listOf(
                "showRating" to "false",
                "showRefereeCoaching" to "true",
                "showCoachingSheet" to "false",
                "showMatchConsole" to "false",
                "showRoster" to "false",
                "showMatchSetup" to "false",
                "showDrawingPad" to "true",
                "showTacticBoard" to "true",
            )
            "rating" -> listOf(
                "showRating" to "true",
                "showRefereeCoaching" to "false",
                "showCoachingSheet" to "false",
                "showMatchConsole" to "false",
                "showRoster" to "false",
                "showMatchSetup" to "false",
                "showDrawingPad" to "false",
                "showTacticBoard" to "false",
            )
            else -> listOf(
                "showRating" to "true",
                "showRefereeCoaching" to "true",
                "showCoachingSheet" to "true",
                "showMatchConsole" to "true",
                "showRoster" to "true",
                "showMatchSetup" to "true",
                "showDrawingPad" to "true",
                "showTacticBoard" to "true",
            )
        }
        file.writeText(
            """
            |package de.exhumedo.kmp.handball_support.config
            |
            |/** Compile-time feature flags for the active build variant. */
            |internal object AppVariant {
${values.joinToString("\n") { (k, v) -> "            |    const val $k: Boolean = $v" }.prependIndent()}
            |}
            """.trimMargin()
        )
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm()

    js {
        browser()
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp.mpp)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.ktor.client.core.mpp)
            implementation(libs.ktor.client.contentNegotiation.mpp)
            implementation(libs.ktor.serialization.kotlinxJson.mpp)
            implementation(libs.kotlinx.serialization.json.mpp)
            implementation(libs.kotlinx.datetime)
            implementation(projects.shared)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin.mpp)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.ktor.client.cio.mpp)
        }
        webMain.dependencies {
            implementation(libs.ktor.client.js.mpp)
        }
    }

    // Make all source sets depend on the generated AppVariant.kt.
    sourceSets.commonMain {
        kotlin.srcDir(generateAppVariant.map { it.outputs.files.singleFile })
    }
}

android {
    namespace = "de.exhumedo.kmp.handball_support"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "de.exhumedo.kmp.handball_support"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        val apiBaseUrl = (project.findProperty("apiBaseUrl") as? String)
            ?: System.getenv("API_BASE_URL")
            ?: "http://10.0.2.2:8090"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }
    buildFeatures {
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "de.exhumedo.kmp.handball_support.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "de.exhumedo.kmp.handball_support"
            packageVersion = "1.0.0"
        }
    }
}
