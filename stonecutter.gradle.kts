plugins {
    id("dev.kikugie.stonecutter")
    id("dev.architectury.loom") version "1.10.+" apply false
}

stonecutter active "1.21.8-fabric" /* [SC] DO NOT EDIT */

stonecutter parameters {
    val loader = node.project.property("loom.platform").toString()
    constants.match(loader, "fabric", "forge", "neoforge")
}
