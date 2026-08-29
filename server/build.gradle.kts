plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktor)
    application
}

group = "de.exhumedo.kmp.handball_support"
version = "1.0.0"
application {
    mainClass.set("de.exhumedo.kmp.handball_support.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(libs.auth0.jwt)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.clientCio)
    implementation(libs.ktor.clientContentNegotiation)
    implementation(libs.ktor.clientCore)
    implementation(libs.ktor.serializationKotlinxJson)
    implementation(libs.ktor.serverAuth)
    implementation(libs.ktor.serverAuthJwt)
    implementation(libs.ktor.serverCallLogging)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serverCors)
    implementation(libs.ktor.serverDefaultHeaders)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverStatusPages)
    implementation(libs.logback)
    implementation(libs.sqlite.jdbc)
    implementation(projects.shared)
    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.ktor.clientMock)
    testImplementation(libs.ktor.serverTestHost)
}
