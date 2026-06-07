import kotlin.text.Regex
import kotlin.text.RegexOption

fun cleanStreamContent(currentText: String): String {
    var text = currentText

    text = text.replace("\\n", "\n")

    text = text.trim()
    if (text.startsWith("```json")) {
        text = text.removePrefix("```json").trimStart()
    } else if (text.startsWith("```")) {
        text = text.removePrefix("```").trimStart()
    }

    if (text.startsWith("{")) {
        text = text.removePrefix("{").trimStart()
    }

    if (text.endsWith("```")) {
        text = text.removeSuffix("```").trimEnd()
    }
    if (text.endsWith("}")) {
        text = text.removeSuffix("}").trimEnd()
    }

    val keysPattern = Regex("\"?(distortion|alternative|action|questions)\"?\\s*:\\s*\"?", RegexOption.IGNORE_CASE)
    text = text.replace(keysPattern, "")

    val separatorPattern = Regex("(?<!\\\\\\\\)\",\\s*")
    text = text.replace(separatorPattern, "\n\n")

    text = text.trim()
    if (text.endsWith("\"") && !text.endsWith("\\\"")) {
        text = text.removeSuffix("\"")
    }

    text = text.replace("\\\"", "\"")

    return text.trim()
}

fun main() {
    println(cleanStreamContent("{\"distortion\": \"灾难化思维\", \"alternative\": \"其实没那么糟\"}"))
    println("---")
    println(cleanStreamContent("```json\n{\n  \"action\": \"深呼吸\"\n}\n```"))
    println("---")
    println(cleanStreamContent("\"distort"))
    println("---")
    println(cleanStreamContent("distortion\": \"Cat"))
    println("---")
    println(cleanStreamContent("{\"distortion\": \"He said \\\"hello\\\", and left.\", \"alternative\": \"其实没那么糟\"}"))
}
