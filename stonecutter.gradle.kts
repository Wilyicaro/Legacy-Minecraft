plugins {
    id("dev.kikugie.stonecutter")
    id("dev.architectury.loom") version "1.13.+" apply false
}

stonecutter active "1.21.10-fabric" /* [SC] DO NOT EDIT */

stonecutter parameters {
    val loader = node.project.property("loom.platform").toString()
    constants.match(loader, "fabric", "forge", "neoforge")
}
