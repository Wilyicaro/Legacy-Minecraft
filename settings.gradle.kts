pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.kikugie.dev/snapshots")
        maven("https://maven.kikugie.dev/releases")
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9"
}

stonecutter {
    create(rootProject) {
        version("1.21.10-fabric", "1.21.10")
        version("1.21.10-forge", "1.21.10")
        version("1.21.10-neoforge", "1.21.10")
        version("1.21.11-fabric", "1.21.11")
        version("1.21.11-forge", "1.21.11")
        version("1.21.11-neoforge", "1.21.11")
        vcsVersion = "1.21.10-fabric"
    }
}

rootProject.name = "Legacy4J"
