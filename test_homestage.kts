sealed class HomeStage(val order: Int) : Comparable<HomeStage> {
    data object QuickStart : HomeStage(0)
    data object WritingThought : HomeStage(1)
    data object ChoosingMode : HomeStage(2)
    data object ChoosingMood : HomeStage(3)
    data object ReviewReady : HomeStage(4)

    override fun compareTo(other: HomeStage): Int {
        return this.order.compareTo(other.order)
    }
}

fun main() {
    val stage: HomeStage = HomeStage.ChoosingMood
    println("ChoosingMode: ${stage >= HomeStage.ChoosingMode}")
    println("ChoosingMood: ${stage >= HomeStage.ChoosingMood}")
}

main()
