@file:Suppress("unused", "DuplicatedCode")

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import dev.kikugie.fletching_table.extension.FletchingTableExtension
import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import me.modmuss50.mpp.ModPublishExtension
import me.modmuss50.mpp.ReleaseType
import org.gradle.api.JavaVersion
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.Copy
import org.gradle.internal.extensions.stdlib.toDefaultLowerCase
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.plugins.ide.idea.model.IdeaModel
import java.io.File
import java.util.*
import javax.inject.Inject

fun Project.prop(name: String): String = (findProperty(name) ?: "") as String

fun Project.env(variable: String): String? = providers.environmentVariable(variable).orNull

fun Project.envTrue(variable: String): Boolean = env(variable)?.toDefaultLowerCase() == "true"

fun RepositoryHandler.strictMaven(
	url: String, vararg groups: String, configure: MavenArtifactRepository.() -> Unit = {}
) = exclusiveContent {
	forRepository { maven(url) { configure() } }
	filter { groups.forEach(::includeGroup) }
}

abstract class ModPlatformPlugin @Inject constructor() : Plugin<Project> {
	override fun apply(project: Project) = with(project) {
		val inferredLoader = project.buildFile.name.substringAfter('.').replace(".gradle.kts", "")
		val inferredLoaderIsFabric = inferredLoader == "fabric"

		val extension = extensions.create("platform", ModPlatformExtension::class.java).apply {
			loader.convention(inferredLoader)
			jarTask.convention(if (inferredLoaderIsFabric) "remapJar" else "jar")
			sourcesJarTask.convention(if (inferredLoaderIsFabric) "remapSourcesJar" else "sourcesJar")
		}

		val stonecutter = extensions.getByType<StonecutterBuildExtension>()

		extension.requiredJava.set(
			when {
				stonecutter.eval(stonecutter.current.version, ">=26.1") -> JavaVersion.VERSION_25
				stonecutter.eval(stonecutter.current.version, ">=1.20.6") -> JavaVersion.VERSION_21
				stonecutter.eval(stonecutter.current.version, ">=1.18") -> JavaVersion.VERSION_17
				stonecutter.eval(stonecutter.current.version, ">=1.17") -> JavaVersion.VERSION_16
				else -> JavaVersion.VERSION_1_8
			}
		)

		configureJava(stonecutter, extension.requiredJava.get())

		extension.commonAccessWidener.convention("aw/${prop("mod_id")}")

		val awPath = "src/main/resources/${extension.commonAccessWidener.get()}"

		extension.awFile.convention("${awPath}.accesswidener")
		extension.atFile.convention("${awPath}.cfg")

		if (!inferredLoaderIsFabric && inferredLoader != "fabricMC") {
			val task = project.tasks.register("convertAccessWidener", ConvertAccessWidenerToTransformerTask::class.java) {
				group = "build setup"
				description = "Converts a Fabric .accesswidener file to a Forge access transformer .cfg"

				inputFile.set(rootProject.layout.projectDirectory.file(extension.awFile))
				outputFile.set(project.layout.projectDirectory.file(extension.atFile))
			}

			project.tasks.matching { it.name == "stonecutterPrepare" || it.name == "processResources" }.configureEach {
				dependsOn(task)
			}
			project.tasks.matching { it.name == "sourcesJar" }.configureEach {
				mustRunAfter(task)
			}

			// That isn't correct, but it works lol
			task.get().convert()
		}

		listOf(
			"org.jetbrains.kotlin.jvm",
			"com.google.devtools.ksp",
			"dev.kikugie.fletching-table",
			"com.vanniktech.maven.publish",
		).forEach { apply(plugin = it) }

		afterEvaluate {
			configureProject(extension)
		}
	}

	private fun Project.configureProject(extension: ModPlatformExtension) {
		val loader = extension.loader.get()
		val isFabric = loader == "fabric"
		val isNeoForge = loader == "neoforge"
		val isForge = loader == "forge"

		val modId = prop("mod_id")
		val modVersion = prop("mod_version")
		val channelTag = prop("stage")

		val stonecutter = extensions.getByType<StonecutterBuildExtension>()

		listOf(
			"java",
			"me.modmuss50.mod-publish-plugin",
			"idea",
		).forEach { apply(plugin = it) }

		val mcVersion = stonecutter.current.version

		version = "$mcVersion-$modVersion-$loader"

		if (isFabric) {
			extension.dependencies { required("java") { versionRange = ">=${extension.requiredJava.get().majorVersion}" } }
		}

		configureFletchingTable()
		configureStonecutter()
		configureJarTask(modId, prop("archives_base_name"), loader)
		configureIdea()
		configureProcessResources(stonecutter, isFabric, isNeoForge, isForge, modId, modVersion, mcVersion, extension, extension.requiredJava.get())
		registerBuildAndCollectTask(extension, modVersion)
		configurePublishing(extension, loader, stonecutter, modVersion, channelTag, version.toString())
	}

	private fun Project.configureJarTask(modId: String, archivesBaseName: String, loader: String) {
		val isForge = loader == "forge"

		tasks.withType<Jar>().configureEach {
			archiveBaseName.set(archivesBaseName)
			if (isForge) {
				manifest.attributes(
					"MixinConfigs" to "${modId}.mixins.json"
				)
			}
		}
	}

	private fun Project.configureProcessResources(
		stonecutter: StonecutterBuildExtension,
		isFabric: Boolean,
		isNeoForge: Boolean,
		isForge: Boolean,
		modId: String,
		modVersion: String,
		mcVersion: String,
		extension: ModPlatformExtension,
		requiredJava: JavaVersion
	) {
		tasks.named<ProcessResources>("processResources") {
			dependsOn(tasks.named("stonecutterGenerate"))
			dependsOn("kspKotlin")

			filesMatching("*.mixins.json") { expand("java" to "JAVA_${requiredJava.majorVersion}") }

			var contributors = prop("mod_credits")
			var authors = prop("mod_authors")
			var issuesUrl = prop("mod_issues")
			if (issuesUrl == "") issuesUrl = prop("mod_source") + "/issues"

			if (isFabric) {
				contributors = contributors.replace(", ", "\", \"")
				authors = authors.replace(", ", "\", \"")
			}

			val dependencies = buildDependenciesBlock(isFabric, modId, extension.dependencies)

			var versionRange = prop("mc_version_range")

			if (isFabric) {
				versionRange = versionRange.split(",").joinToString("\",\"")
			}

			val props = mapOf(
				"version" to modVersion,
				"minecraft" to mcVersion,
				"mc_version_range" to versionRange,
				"mod_id" to modId,
				"mod_name" to prop("mod_name"),
				"mod_group" to prop("mod_group"),
				"mod_authors" to authors,
				"mod_credits" to contributors,
				"mod_license" to prop("mod_license"),
				"mod_description" to prop("mod_description"),
				"mod_issues" to issuesUrl,
				"fabric_api_version" to prop("fabric_api_version"),
				"factory_api_version" to prop("factory_api_version"),
				"aw" to extension.commonAccessWidener.get(),
//				"homepage_url" to prop("mod_homepage_url"),
				"mod_source" to prop("mod_source"),
//				"discord_url" to prop("mod_discord"),
				"dependencies" to dependencies
			)

			when {
				isFabric -> {
					filesMatching("fabric.mod.json") { expand(props) }
					exclude("META-INF/mods.toml", "META-INF/neoforge.mods.toml", "aw/*.cfg", ".cache", "pack.mcmeta")
				}

				isNeoForge -> {
					filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
					if (stonecutter.eval(stonecutter.current.version, "<=1.20.4")){
						filesMatching("META-INF/neoforge.mods.toml") {
							copyTo(File(outputs.files.asPath, "META-INF/mods.toml"))
							exclude()
						}
					} else {
						exclude("META-INF/mods.toml")
					}
					exclude("fabric.mod.json", "aw/*.accesswidener", ".cache", "pack.mcmeta")
				}

				isForge -> {
					filesMatching("META-INF/mods.toml") { expand(props) }
					exclude("META-INF/neoforge.mods.toml", "fabric.mod.json", "aw/*.accesswidener", ".cache")
				}
			}
		}
	}

	private fun buildDependenciesBlock(
		isFabric: Boolean, modId: String, deps: DependenciesConfig
	): String = if (isFabric) {
		buildString {
			fun joinGroup(
				name: String, container: NamedDomainObjectContainer<Dependency>
			): String? {
				if (container.isEmpty()) return null
				val entries = container.joinToString(",\n    ") {
					"\"${it.modid.get()}\": \"${it.versionRange.get()}\""
				}
				return "\n  \"$name\": {\n    $entries\n  }"
			}

			val groups = listOfNotNull(
				joinGroup("depends", deps.required),
				joinGroup("recommends", deps.optional),
				joinGroup("breaks", deps.incompatible)
			)

			append(groups.joinToString(","))
		}
	} else {
		buildString {
			fun appendBlock(container: NamedDomainObjectContainer<Dependency>, type: String) {
				container.forEach {
					appendLine(
						"""

						[[dependencies.$modId]]
						modId = "${it.modid.get()}"
						side = "${it.environment.get().uppercase(Locale.getDefault())}"
                        versionRange = "${it.forgeVersionRange.get()}"
						mandatory = ${if (type == "required") "true" else "false"}
                        type = "$type"
						""".replace("                  ", "").trimIndent()
					)
				}
			}

			appendBlock(deps.required, "required")
			appendBlock(deps.optional, "optional")
			appendBlock(deps.incompatible, "incompatible")
		}
	}

	private fun Project.configureJava(stonecutter: StonecutterBuildExtension, requiredJava: JavaVersion) {
		extensions.configure<JavaPluginExtension>("java") {
			withSourcesJar()
			//withJavadocJar()
			toolchain.languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
			sourceCompatibility = requiredJava
			targetCompatibility = requiredJava
		}
	}

	private fun Project.configureIdea() {
		extensions.configure<IdeaModel>("idea") {
			module {
				isDownloadJavadoc = true
				isDownloadSources = true
			}
		}
	}

	private fun Project.configureStonecutter() {
		extensions.configure<StonecutterBuildExtension>("stonecutter") {
			replacements {
				string(eval(current.version, ">=1.21.11")) {
					replace("net.minecraft.resources.ResourceLocation", "net.minecraft.resources.Identifier")
				}
				string(eval(current.version, ">=1.21.11")) {
					replace("net/minecraft/resources/ResourceLocation", "net/minecraft/resources/Identifier")
				}
				string(eval(current.version, ">=1.21.11")) {
					replace("writeResourceLocation", "writeIdentifier")
				}
				string(eval(current.version, ">=1.21.11")) {
					replace("readResourceLocation", "readIdentifier")
				}
				string(eval(current.version, ">=1.21.11")) {
					replace("ResourceLocationArgument", "IdentifierArgument")
				}
				string(eval(current.version, ">=1.21.11")) {
					replace("import net.minecraft.Util;", "import net.minecraft.util.Util;")
				}
				string(eval(current.version, ">=26.1"), "!renaming_26") {
					replace("net.minecraft.client.gui.GuiGraphics", "net.minecraft.client.gui.GuiGraphicsExtractor")
				}
				string(eval(current.version, ">=26.1"), "!renaming_26") {
					replace("net/minecraft/client/gui/GuiGraphics", "net/minecraft/client/gui/GuiGraphicsExtractor")
				}
				string(eval(current.version, ">=26.1"), "!renaming_26") {
					replace("renderHotbarAndDecorations", "extractHotbarAndDecorations")
				}
				string(eval(current.version, ">=26.1"), "!renaming_26") {
					replace("net/minecraft/client/gui/contextualbar/ContextualBarRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", "net/minecraft/client/gui/contextualbar/ContextualBarRenderer;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V")
				}
				string(eval(current.version, ">=26.1"), "!renaming_26") {
					replace("net/minecraft/client/gui/contextualbar/ContextualBarRenderer;renderBackground(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", "net/minecraft/client/gui/contextualbar/ContextualBarRenderer;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V")
				}
				string(eval(current.version, ">=26.1"), "!renaming_26") {
					replace("renderContextualInfoBarBackground", "extractContextualInfoBarBackground")
				}
				string(eval(current.version, ">=26.1"), "!renaming_26") {
					replace("renderContextualInfoBar", "extractContextualInfoBar")
				}
			}
		}
	}

	private fun Project.configureFletchingTable() {
		extensions.configure<FletchingTableExtension> {
			j52j.register("main") {
				extension("json", "**/*.json5")
			}
			mixins.create("main").apply {
				mixin("default", "${prop("mod_id")}.mixins.json")
			}
		}
	}

	private fun Project.registerBuildAndCollectTask(extension: ModPlatformExtension, modVersion: String) {
		tasks.register<Copy>("buildAndCollect") {
			group = "build"
			from(
				tasks.named(extension.jarTask.get()),
				tasks.named(extension.sourcesJarTask.get())
			)
			into(rootProject.layout.buildDirectory.file("libs/$modVersion"))
			dependsOn("build")
		}
	}

	private fun Project.configurePublishing(
		ext: ModPlatformExtension,
		loader: String,
		stonecutter: StonecutterBuildExtension,
		modVersion: String,
		channelTag: String,
		fullVersion: String,
	) {
		val additionalVersions = (findProperty("publish.additionalVersions") as String?)?.split(',')?.map(String::trim)
			?.filter(String::isNotEmpty).orEmpty()

		val releaseType = ReleaseType.of(
			channelTag.substringAfter('-').substringBefore('.').ifEmpty { "stable" })

		extensions.configure<ModPublishExtension>("publishMods") {
			val mrStaging = envTrue("TEST_PUBLISHING_WITH_MR_STAGING")

			val modrinthAccessToken = prop("MODRINTH_TOKEN")
			val curseforgeAccessToken = prop("CURSE_API_KEY")

			val targetName = ext.jarTask.get()

			val jarTask = tasks.named(targetName).map { it as Jar }
			val srcJarTask = tasks.named(ext.sourcesJarTask.get()).map { it as Jar }
			val currentVersion = stonecutter.current.version
			val deps = ext.dependencies

			file.set(jarTask.flatMap(Jar::getArchiveFile))
			additionalFiles.from(srcJarTask.flatMap(Jar::getArchiveFile))
			type = releaseType
			version = fullVersion
			changelog.set(rootProject.file("src/main/resources/assets/legacy/changelog/en_us.txt").readText())
			modLoaders.add(loader)

			displayName = "${prop("mod_name")} $modVersion ${loader.replaceFirstChar(Char::titlecase)} $currentVersion"

			modrinth(deps, currentVersion, additionalVersions, mrStaging, modrinthAccessToken)
			if (!mrStaging) curseforge(deps, currentVersion, additionalVersions, false, curseforgeAccessToken)
		}

		extensions.configure<MavenPublishBaseExtension>("mavenPublishing") {
			coordinates(groupId = prop("maven_group"), artifactId = "${prop("mod_id")}-${loader}", version = "${stonecutter.current.version}-${modVersion}")

			pom {
				name = "${prop("mod_name")} [${loader.replaceFirstChar(Char::titlecase)}]"
				description = prop("mod_description")
				url = prop("mod_source")
				scm {
					url = prop("mod_source")
					connection = prop("mod_source").replace("https", "scm:git:git") + ".git"
					developerConnection = prop("mod_source").replace("https://github.com/", "scm:git:git@github.com:") + ".git"
				}
				issueManagement {
					system = "github"
					url = prop("mod_issues")
				}
				licenses {
					license {
						name = "MPL-2"
						url = "https://www.mozilla.org/en-US/MPL/2.0/"
					}
				}
				developers {
					developer {
						id = prop("mod_authors").toDefaultLowerCase()
						name = prop("mod_authors")
					}
				}
			}
		}
		extensions.configure<PublishingExtension>("publishing") {
			repositories {
				maven {
					repositories {
						maven {
							name = "ModResources"
							val modResourcesURL = project.findProperty("MOD_RESOURCES") ?: System.getenv("MOD_RESOURCES")
							if (modResourcesURL != null)
								url = uri(modResourcesURL)
						}
					}
				}
			}
		}
	}

	fun whenNotNull(stringProp: Property<String>, action: (String) -> Unit) {
		if (!stringProp.orNull.isNullOrBlank()) action(stringProp.get())
	}

	private fun ModPublishExtension.modrinth(
		deps: DependenciesConfig,
		currentVersion: String,
		additionalVersions: List<String>,
		staging: Boolean,
		acesssToken: String?
	) = modrinth {
		if (staging) apiEndpoint = "https://staging-api.modrinth.com/v2"
		projectId = project.prop("modrinth_id")
		accessToken = acesssToken
		minecraftVersions.addAll(listOf(currentVersion) + additionalVersions)

		if (!staging) {
			deps.required.forEach { dep -> whenNotNull(dep.modrinth) { requires(it) } }
			deps.optional.forEach { dep -> whenNotNull(dep.modrinth) { optional(it) } }
			deps.incompatible.forEach { dep -> whenNotNull(dep.modrinth) { incompatible(it) } }
			deps.embeds.forEach { dep -> whenNotNull(dep.modrinth) { embeds(it) } }
		}
	}

	private fun ModPublishExtension.curseforge(
		deps: DependenciesConfig,
		currentVersion: String,
		additionalVersions: List<String>,
		staging: Boolean,
		acesssToken: String?
	) = curseforge {
		projectId = project.prop("curseforge_id")
		accessToken = acesssToken
		minecraftVersions.addAll(listOf(currentVersion) + additionalVersions)

		deps.required.forEach { dep -> whenNotNull(dep.curseforge) { requires(it) } }
		deps.optional.forEach { dep -> whenNotNull(dep.curseforge) { optional(it) } }
		deps.incompatible.forEach { dep -> whenNotNull(dep.curseforge) { incompatible(it) } }
		deps.embeds.forEach { dep -> whenNotNull(dep.curseforge) { embeds(it) } }
	}
}
