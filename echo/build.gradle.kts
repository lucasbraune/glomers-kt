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
    mainClass.set("io.github.lucasbraune.glomers.echo.AppKt")
}

val maelstromBinary = project.rootDir.resolve("maelstrom/maelstrom")

tasks.register<Exec>("runMaelstromTest") {
    dependsOn(":echo:installDist")

    val broadcastBinary = project.rootDir.resolve("echo/build/install/echo/bin/echo")

    commandLine(
        maelstromBinary.absolutePath, "test",
        "-w", "echo",
        "--bin", broadcastBinary.absolutePath,
        "--node-count", "1",
        "--time-limit", "10",
    )
}

tasks.register<Exec>("runMaelstromServe") {
    commandLine(maelstromBinary.absolutePath, "serve")
}
