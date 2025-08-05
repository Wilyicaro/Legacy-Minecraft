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
    id("dev.kikugie.stonecutter") version "0.7.6"
}

stonecutter {
    create(rootProject) {
        vers("1.21.8-fabric", "1.21.8")
        vers("1.21.8-forge", "1.21.8")
        vers("1.21.8-neoforge", "1.21.8")
        vcsVersion = "1.21.8-fabric"
    }
}

rootProject.name = "Legacy4J"
