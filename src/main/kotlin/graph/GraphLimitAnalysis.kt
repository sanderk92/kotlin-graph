package graph

import java.math.BigDecimal

data class GraphLimitAnalysis(
    val limit: BigDecimal,
    val analyses: Map<Int, BarLimitAnalysis> = emptyMap(),
) {
    fun hasViolations(): Boolean = analyses
        .any { (_, analysis) -> analysis is BarExceedsLimit }

    fun getViolatingBars(): Map<Int, BarExceedsLimit> = analyses
        .filter { (_, analysis) -> analysis is BarExceedsLimit }
        .mapValues { (_, analysis) -> analysis as BarExceedsLimit }
}

sealed interface BarLimitAnalysis {
    val included: Bar
}

data class BarRespectsLimit(
    override val included: Bar = Bar(),
) : BarLimitAnalysis

data class BarExceedsLimit(
    override val included: Bar = Bar(),
    val partiallyExcluded: Pair<Block, BigDecimal>? = null,
    val excluded: Bar = Bar(),
) : BarLimitAnalysis
