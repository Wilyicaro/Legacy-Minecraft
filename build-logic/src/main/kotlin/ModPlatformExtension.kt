@file:Suppress("unused")

import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

abstract class ModPlatformExtension {

	abstract val requiredJava: Property<JavaVersion>
	abstract val loader: Property<String>
	abstract val jarTask: Property<String>
	abstract val sourcesJarTask: Property<String>
	abstract val commonAccessWidener: Property<String>
	abstract val awFile: Property<String>
	abstract val atFile: Property<String>

	@get:Nested
	abstract val dependencies: DependenciesConfig

	init {
		requiredJava.convention(JavaVersion.VERSION_21)
	}

	fun dependencies(action: Action<DependenciesConfig>) {
		action.execute(dependencies)
	}
}

abstract class DependenciesConfig @Inject constructor(val objects: ObjectFactory) {

	private fun container() = objects.domainObjectContainer(Dependency::class.java)

	val required: NamedDomainObjectContainer<Dependency> = container()
	val optional: NamedDomainObjectContainer<Dependency> = container()
	val incompatible: NamedDomainObjectContainer<Dependency> = container()
	val embeds: NamedDomainObjectContainer<Dependency> = container()

	fun required(modid: String, action: Action<Dependency>): Dependency? = required.create(modid, action)
	fun optional(modid: String, action: Action<Dependency>): Dependency? = optional.create(modid, action)
	fun incompatible(modid: String, action: Action<Dependency>): Dependency? = incompatible.create(modid, action)
	fun embeds(modid: String, action: Action<Dependency>): Dependency? = embeds.create(modid, action)
}

abstract class Dependency @Inject constructor(val name: String) {

	abstract val modid: Property<String>
	abstract val modrinth: Property<String>
	abstract val curseforge: Property<String>
	abstract val versionRange: Property<String>
	abstract val forgeVersionRange: Property<String>
	abstract val environment: Property<String>

	init {
		modid.convention(name)
		versionRange.convention("*")
		forgeVersionRange.convention("(,]")
		environment.convention("both")
	}

	fun slug(slug: String) {
		modrinth.set(slug)
		curseforge.set(slug)
	}

	fun slug(modrinthSlug: String? = null, curseforgeSlug: String? = null) {
		if (modrinthSlug != null) { modrinth.set(modrinthSlug) }
		if (curseforgeSlug != null) { curseforge.set(curseforgeSlug) }
	}
}
