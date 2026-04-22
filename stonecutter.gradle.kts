plugins {
    alias(libs.plugins.stonecutter)
    alias(libs.plugins.dotenv)
    alias(libs.plugins.fabric.loom).apply(false)
    alias(libs.plugins.neoforged.moddev).apply(false)
    alias(libs.plugins.jsonlang.postprocess).apply(false)
    alias(libs.plugins.mod.publish.plugin).apply(false)
    alias(libs.plugins.kotlin.jvm).apply(false)
    alias(libs.plugins.devtools.ksp).apply(false)
    alias(libs.plugins.fletching.table).apply(false)
    alias(libs.plugins.legacyforge.moddev).apply(false)
    alias(libs.plugins.vanniktech.maven.publish).apply(false)
}

stonecutter active file(".sc_active_version")

stonecutter parameters {
    constants.match(node.metadata.project.substringAfterLast('-'), "fabric", "neoforge", "forge")
    filters.include("**/*.fsh", "**/*.vsh")
}
