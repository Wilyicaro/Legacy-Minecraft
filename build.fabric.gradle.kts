plugins {
	id("mod-platform")
	id("fabric-loom")
}

platform {
	loader = "fabric"
	dependencies {
		required("minecraft") {
			versionRange = stonecutter.current.version
		}
		required("fabric-api") {
			slug("fabric-api")
			versionRange = ">=${prop("fabric_api_version")}"
		}
		required("fabricloader") {
			versionRange = ">=${libs.fabric.loader.get().version}"
		}
		optional("modmenu") {}
	}
}

configurations.configureEach {
	resolutionStrategy {
		force("net.fabricmc:fabric-loader:${prop("fabric_loader_version")}")
	}
}

loom {
	accessWidenerPath = rootProject.file(platform.awFile)
	runs.named("client") {
		client()
		ideConfigGenerated(true)
		runDir = "run/"
		environment = "client"
		programArgs("--username=Dev")
		configName = "Fabric Client"
	}
	runs.named("server") {
		server()
		ideConfigGenerated(true)
		runDir = "run/"
		environment = "server"
		configName = "Fabric Server"
	}

	mixin {
		useLegacyMixinAp = true
		defaultRefmapName = "${prop("mod_id")}.refmap.json"
	}
}

fabricApi {
	configureDataGeneration {
		outputDirectory = file("${rootDir}/versions/datagen/${stonecutter.current.version.split("-")[0]}/src/main/generated")
		client = true
	}
}

repositories {
	mavenCentral()
	strictMaven("https://maven.terraformersmc.com/", "com.terraformersmc") { name = "TerraformersMC" }
	strictMaven("https://api.modrinth.com/maven", "maven.modrinth") { name = "Modrinth" }
	maven("https://raw.githubusercontent.com/Kyubion-Studios/Mod-Resources/main/maven/") { name = "Kyubion Mod Resources" }
	maven("https://maven.isxander.dev/releases")
}

dependencies {
	minecraft("com.mojang:minecraft:${stonecutter.current.version}")
	mappings(
		loom.layered {
			officialMojangMappings()
			if (hasProperty("deps.parchment")) parchment("org.parchmentmc.data:parchment-${prop("deps.parchment")}@zip")
		})
	modImplementation(libs.fabric.loader)
	modImplementation("net.fabricmc.fabric-api:fabric-api:${prop("fabric_api_version")}")
	modImplementation("com.terraformersmc:modmenu:${prop("modmenu_version")}")
	modApi(include(prop("sdl_dependency")) as Any)
	modApi("wily.factory_api:factory_api-fabric:${stonecutter.current.version}-${prop("factory_api_version")}")

	modCompileOnly("maven.modrinth:world-host:${prop("world_host_version")}")
	modCompileOnly("maven.modrinth:vivecraft:${prop("vivecraft_version")}")
	modCompileOnly("maven.modrinth:sodium:${prop("sodium_version")}")
	modCompileOnly("maven.modrinth:iris:${prop("iris_version")}")
	modCompileOnly("maven.modrinth:nostalgic-tweaks:${prop("nt_version")}")

	implementation(libs.moulberry.mixinconstraints)
	include(libs.moulberry.mixinconstraints)
	api(include("org.apache.httpcomponents:httpclient:4.5.14") as Any)
}

tasks.withType<Javadoc> {
	enabled = false
}