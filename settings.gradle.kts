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
        vers("1.20.1-fabric", "1.20.1")
        vers("1.20.1-forge", "1.20.1")
        vers("1.20.4-fabric", "1.20.4")
        vers("1.20.4-forge", "1.20.4")
        vers("1.20.4-neoforge", "1.20.4")
        vers("1.21.1-fabric", "1.21.1")
        vers("1.21.1-forge", "1.21.1")
        vers("1.21.1-neoforge", "1.21.1")
        vers("1.21.3-fabric", "1.21.3")
        vers("1.21.3-forge", "1.21.3")
        vers("1.21.3-neoforge", "1.21.3")
        vers("1.21.4-fabric", "1.21.4")
        vers("1.21.4-forge", "1.21.4")
        vers("1.21.4-neoforge", "1.21.4")
        vers("1.21.5-fabric", "1.21.5")
        vers("1.21.5-forge", "1.21.5")
        vers("1.21.5-neoforge", "1.21.5")
        vcsVersion = "1.21.1-fabric"
    }
}

rootProject.name = "Legacy4J"
