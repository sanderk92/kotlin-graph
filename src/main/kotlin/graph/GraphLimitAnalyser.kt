package graph

import java.math.BigDecimal


class GraphLimitAnalyser {

    /**
     * A limit analysis checks for each [Block] in each [Bar] of a [Graph] whether it is included when the given limit
     * is enforced, meaning its value including the values of all blocks supporting it results in a value below the
     * limit.
     *
     * Included (within limit), and when applicable partial (crossing limit) and excluded blocks (outside of limit) are
     * detected.
     */
    fun analyse(graph: Graph, limit: BigDecimal): GraphLimitAnalysis =
        graph.bars
            .mapValues { (_, bar) -> analyse(bar, limit) }
            .let { barAnalyses ->
                GraphLimitAnalysis(
                    limit = limit,
                    analyses = barAnalyses,
                )
            }

    private fun analyse(barUnderTest: Bar, limit: BigDecimal): BarLimitAnalysis {
        return when {
            barUnderTest.isEmpty() -> BarRespectsLimit()
            limit < BigDecimal.ZERO -> BarExceedsLimit(
                included = Bar(),
                excluded = barUnderTest,
            )
            else -> {
                val (first, rest) = barUnderTest.blocks.decons()
                analyse(Bar(rest), first, Bar(), limit)
            }
        }
    }

    /**
     * Recursively iterates over the blocks in the remainder bar. [remainderBar] + [block] + [currentBar] is the total
     * bar we are analysing. Every iteration, the [currentBar] grows with the [block] and the [remainderBar] is split
     * into the [block] and [remainderBar] for the next iteration.
     */
    private tailrec fun analyse(
        remainderBar: Bar,
        block: Block,
        currentBar: Bar,
        limit: BigDecimal,
    ): BarLimitAnalysis {
        val nextBar: Bar = currentBar + block
        return when {
            // The subject bar has a block cut off by the limit
            nextBar.power > limit -> BarExceedsLimit(
                included = currentBar,
                partiallyExcluded = block to nextBar.power - limit,
                excluded = remainderBar,
            )

            // The subject bar matches the limit exactly
            nextBar.power == limit && remainderBar.isEmpty() -> BarRespectsLimit(
                included = nextBar,
            )

            // The subject bar has a subset of blocks that matches the limit exactly
            nextBar.power == limit && remainderBar.isNotEmpty() -> BarExceedsLimit(
                included = nextBar,
                excluded = remainderBar,
            )

            remainderBar.isEmpty() -> BarRespectsLimit(
                included = nextBar,
            )

            else -> {
                val (first, rest) = remainderBar.blocks.decons()
                analyse(Bar(rest), first, nextBar, limit)
            }
        }
    }
}

private fun <I> List<I>.decons(): Pair<I, List<I>> = first() to drop(1)
