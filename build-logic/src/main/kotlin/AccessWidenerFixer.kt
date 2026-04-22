import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

abstract class ConvertAccessWidenerTask @Inject constructor() : DefaultTask() {

    @get:InputFile
    abstract val inputFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty
}

// Converts access wideners to access transformers in the src resources, before stonecutter pre-processes, so it won't have problems when compiling in (Neo)Forge
abstract class ConvertAccessWidenerToTransformerTask @Inject constructor() : ConvertAccessWidenerTask() {
    // Easier said, than done
    @TaskAction
    fun convert() {
        val input = inputFile.get().asFile
        val output = outputFile.get().asFile

        require(input.exists()) { "Access widener file not found: ${input.absolutePath}" }

        output.parentFile?.mkdirs()

        val lines = input.readLines()

        // Validate header: accessWidener v1/v2 <namespace>
        val header = lines.firstOrNull()
            ?: error("Access widener file is empty: ${input.absolutePath}")
        require(header.startsWith("accessWidener")) {
            "Invalid access widener header: $header"
        }

        val atLines = mutableListOf<String>()
        // Why not?
        atLines += "# Auto-generated from ${input.name} — do not edit manually"

        for ((lineNumber, raw) in lines.withIndex()) {
            val line = raw.trim()

            // Skip header, blank lines and comments
            if (lineNumber == 0) continue
            if (line.isEmpty() || line.startsWith("#")) continue

            val parts = line.split(Regex("\\s+"))
            if (parts.size < 3) {
                logger.warn("[AccessWidenerToAT] Skipping unrecognized line ${lineNumber + 1}: $line")
                continue
            }

            val access = parts[0].removePrefix("transitive-")    // accessible | extendable | mutable
            val type   = parts[1]   // class | method | field
            val owner  = parts[2].replace('/', '.') // net/minecraft/... → net.minecraft...

            val atAccess = when (access) {
                "accessible"  -> "public"
                "extendable", "mutable"  -> "public-f"
                else -> {
                    logger.warn("[AccessWidenerToAT] Unknown access modifier '$access' on line ${lineNumber + 1}, skipping")
                    continue
                }
            }

            val atLine = when (type) {
                "class" -> {
                    // accessible class net/minecraft/Foo
                    // -> public net.minecraft.Foo
                    "$atAccess $owner"
                }
                "method" -> {
                    // accessible method net/minecraft/Foo bar ()V
                    if (parts.size < 5) {
                        logger.warn("[AccessWidenerToAT] Malformed method entry on line ${lineNumber + 1}: $line")
                        continue
                    }
                    val methodName = parts[3]
                    val descriptor = parts[4]
                    // -> public net.minecraft.Foo bar()V
                    "$atAccess $owner $methodName$descriptor"
                }
                "field" -> {
                    // accessible field net/minecraft/Foo bar I
                    if (parts.size < 5) {
                        logger.warn("[AccessWidenerToAT] Malformed field entry on line ${lineNumber + 1}: $line")
                        continue
                    }
                    val fieldName = parts[3]
                    // AT fields don't include the descriptor
                    // -> public net.minecraft.Foo bar
                    "$atAccess $owner $fieldName"
                }
                else -> {
                    logger.warn("[AccessWidenerToAT] Unknown type '$type' on line ${lineNumber + 1}, skipping")
                    continue
                }
            }

            atLines += atLine
        }

        output.writeText(atLines.joinToString("\n"))
        logger.lifecycle("[AccessWidenerToAT] Converted ${input.name} -> ${output.name} (${atLines.size - 1} entries)")
    }
}