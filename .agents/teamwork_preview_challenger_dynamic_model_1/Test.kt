fun main() {
    val id = ""
    val res = id.split("-").joinToString(" ") { part ->
        part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
    println("RES: '$res'")
}
