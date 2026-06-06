package com.henryliu.cbtreframe.shared

sealed class HomeStage(val order: Int) : Comparable<HomeStage> {
    data object QuickStart : HomeStage(0)
    data object WritingThought : HomeStage(1)
    data object ChoosingMood : HomeStage(2)
    data object ReviewReady : HomeStage(3)

    override fun compareTo(other: HomeStage): Int {
        return this.order.compareTo(other.order)
    }
}
