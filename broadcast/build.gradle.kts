/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("glomers.kotlin-application-conventions")
    kotlin("plugin.serialization") version "1.9.22"
}

dependencies {
    implementation(project(":utilities"))
    implementation(project(":protocol"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
}

application {
    // Define the main class for the application.
    mainClass.set("io.github.lucasbraune.broadcast.AppKt")
}

val maelstromBinary = project.rootDir.resolve("maelstrom/maelstrom")

tasks.register<Exec>("runMaelstromTest") {
    dependsOn(":broadcast:installDist")

    val broadcastBinary = project.rootDir.resolve("broadcast/build/install/broadcast/bin/broadcast")

    commandLine(
        maelstromBinary.absolutePath, "test",
        "-w", "broadcast",
        "--bin", broadcastBinary.absolutePath,
        "--node-count", "25",
        "--time-limit", "20",
        "--rate", "100",
        "--latency", "100"
    )
}

tasks.register<Exec>("runMaelstromServe") {
    commandLine(maelstromBinary.absolutePath, "serve")
}