plugins {
	id("mod-platform")
	id("net.fabricmc.fabric-loom")
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
	implementation(libs.fabric.loader)
	implementation("net.fabricmc.fabric-api:fabric-api:${prop("fabric_api_version")}")
	implementation("com.terraformersmc:modmenu:${prop("modmenu_version")}")
	api(include(prop("sdl_dependency")) as Any)
	api("wily.factory_api:factory_api-fabric:${stonecutter.current.version}-${prop("factory_api_version")}")

//	compileOnly("maven.modrinth:world-host:${prop("world_host_version")}")
	compileOnly("maven.modrinth:vivecraft:${prop("vivecraft_version")}")
	compileOnly("maven.modrinth:sodium:${prop("sodium_version")}")
	compileOnly("maven.modrinth:iris:${prop("iris_version")}")
	compileOnly("maven.modrinth:nostalgic-tweaks:${prop("nt_version")}")
	compileOnly("maven.modrinth:bisect-mod:z62iwoR1")

	implementation(libs.moulberry.mixinconstraints)
	include(libs.moulberry.mixinconstraints)
	api(include("org.apache.httpcomponents:httpclient:4.5.14") as Any)
	api(include("org.apache.httpcomponents:httpcore:4.4.16") as Any)
	api(include("commons-logging:commons-logging:1.2") as Any)
	api(include("commons-codec:commons-codec:1.11") as Any)
}

tasks.withType<Javadoc> {
	enabled = false
}
