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
    mainClass.set("uniqueid.AppKt")
}

val maelstromBinary = project.rootDir.resolve("maelstrom/maelstrom")

tasks.register<Exec>("runMaelstromTest") {
    dependsOn(":uniqueid:installDist")

    val uniqueIdBinary = project.rootDir.resolve("uniqueid/build/install/uniqueid/bin/uniqueid")

    commandLine(
        maelstromBinary.absolutePath, "test",
        "-w", "unique-ids",
        "--bin", uniqueIdBinary.absolutePath,
        "--time-limit", "30",
        "--rate", "1000",
        "--node-count", "3",
        "--availability", "total",
        "--nemesis", "partition"
    )
}

tasks.register<Exec>("runMaelstromServe") {
    commandLine(maelstromBinary.absolutePath, "serve")
}
