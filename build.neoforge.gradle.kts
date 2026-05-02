plugins {
	id("mod-platform")
	id("net.neoforged.moddev")
}

platform {
	loader = "neoforge"
	dependencies {
		required("minecraft") {
			forgeVersionRange = "[${prop("mc_version_range")}]"
		}
		required("neoforge") {
			forgeVersionRange = "[1,)"
		}
	}
}

neoForge {
	version = prop("neoforge_version")
	accessTransformers.from(project.file(platform.atFile))
	validateAccessTransformers = true

	if (hasProperty("deps.parchment")) parchment {
		val (mc, ver) = (property("deps.parchment") as String).split(':')
		mappingsVersion = ver
		minecraftVersion = mc
	}

	runs {
		register("client") {
			client()
			gameDirectory = file("run/")
			ideName = "NeoForge Client (${stonecutter.active?.version})"
			programArgument("--username=Dev")
		}
		register("server") {
			server()
			gameDirectory = file("run/")
			ideName = "NeoForge Server (${stonecutter.active?.version})"
		}
	}

	mods {
		register(property("mod_id") as String) {
			sourceSet(sourceSets["main"])
		}
	}
	sourceSets["main"].resources.srcDir("${rootDir}/versions/datagen/${stonecutter.current.version.split("-")[0]}/src/main/generated")
}

repositories {
	mavenCentral()
	strictMaven("https://api.modrinth.com/maven", "maven.modrinth") { name = "Modrinth" }
	maven("https://raw.githubusercontent.com/Kyubion-Studios/Mod-Resources/main/maven/") { name = "Kyubion Mod Resources" }
	maven("https://maven.isxander.dev/releases")
}

dependencies {
	implementation(libs.moulberry.mixinconstraints)
	jarJar(libs.moulberry.mixinconstraints)

	api(jarJar(prop("sdl_dependency")) as Any)
	api("wily.factory_api:factory_api-neoforge:${stonecutter.current.version}-${prop("factory_api_version")}")

	compileOnly("maven.modrinth:world-host:${prop("world_host_version")}")
	compileOnly("maven.modrinth:vivecraft:${prop("vivecraft_version")}")
	compileOnly("maven.modrinth:sodium:${prop("sodium_version")}")
	compileOnly("maven.modrinth:iris:${prop("iris_version")}")
	compileOnly("maven.modrinth:nostalgic-tweaks:${prop("nt_version")}")
	api(jarJar("org.apache.httpcomponents:httpclient:4.5.14") {
		exclude(group = "commons-codec", module = "commons-codec")
	} as Any)
	api(jarJar("org.apache.httpcomponents:httpcore:4.4.16") as Any)
	api(jarJar("commons-logging:commons-logging:1.2") as Any)
}

tasks.withType<Javadoc> {
	enabled = false
}

tasks.named("createMinecraftArtifacts") {
	dependsOn(tasks.named("stonecutterGenerate"))
}
