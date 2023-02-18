package graph

import graph.GraphLimitAnalysisAssert.Companion.assertThat
import org.assertj.core.api.AbstractAssert
import org.junit.jupiter.api.Test
import java.math.BigDecimal

private val blockOfPlusOneMw = DefaultBlock(BigDecimal(1), "context1")
private val blockOfPlusTwoMw = DefaultBlock(BigDecimal(2), "context2")
private val blockOfPlusThreeMw = DefaultBlock(BigDecimal(3), "context3")
private val blockOfPlusFourMw = DefaultBlock(BigDecimal(4), "context4")

private val limitOfPlusFive = BigDecimal(5)
private val limitOfMinusFive = limitOfPlusFive.negate()

class GraphLimitAnalyserTest {

    private val instance = GraphLimitAnalyser()

    @Test
    fun `A bar without blocks and a negative limit is correctly identified`() {
        val graph = someGraph()

        val result = instance.analyse(graph, limitOfMinusFive)

        assertThat(result).respectsLimitAtBar(0).withoutIncluded()
    }

    @Test
    fun `A bar with blocks and a negative limit is correctly identified`() {
        val graph = someGraph(blockOfPlusOneMw, blockOfPlusTwoMw)

        val result = instance.analyse(graph, limitOfMinusFive)

        assertThat(result).exceedsLimitAtBar(0)
            .withoutIncluded()
            .withoutPartial()
            .withExcluded(blockOfPlusOneMw, blockOfPlusTwoMw)
    }

    @Test
    fun `A bar without blocks and an positive limit is correctly identified`() {
        val graph = someGraph()

        val result = instance.analyse(graph, limitOfPlusFive)

        assertThat(result).respectsLimitAtBar(0).withoutIncluded()
    }

    @Test
    fun `A bar with positive blocks not exceeding the specified positive limit is correctly identified`() {
        val graph = someGraph(blockOfPlusOneMw, blockOfPlusTwoMw)

        val result = instance.analyse(graph, limitOfPlusFive)

        assertThat(result).respectsLimitAtBar(0).withIncluded(blockOfPlusOneMw, blockOfPlusTwoMw)
    }

    @Test
    fun `A bar with positive blocks exactly matching the specified positive limit is correctly identified`() {
        val graph = someGraph(blockOfPlusTwoMw, blockOfPlusThreeMw)

        val result = instance.analyse(graph, limitOfPlusFive)

        assertThat(result).respectsLimitAtBar(0).withIncluded(blockOfPlusTwoMw, blockOfPlusThreeMw)
    }

    @Test
    fun `A bar with blocks exceeding the specified positive limit with a partial block is correctly identified`() {
        val graph = someGraph(blockOfPlusOneMw, blockOfPlusTwoMw, blockOfPlusThreeMw, blockOfPlusFourMw)

        val result = instance.analyse(graph, limitOfPlusFive)

        assertThat(result).exceedsLimitAtBar(0)
            .withIncluded(blockOfPlusOneMw, blockOfPlusTwoMw)
            .withPartial(blockOfPlusThreeMw, BigDecimal(1))
            .withExcluded(blockOfPlusFourMw)
    }

    @Test
    fun `A bar with blocks exceeding the specified positive limit without partial block is correctly identified`() {
        val graph = someGraph(blockOfPlusOneMw, blockOfPlusFourMw, blockOfPlusTwoMw)

        val result = instance.analyse(graph, limitOfPlusFive)

        assertThat(result).exceedsLimitAtBar(0)
            .withIncluded(blockOfPlusOneMw, blockOfPlusFourMw)
            .withoutPartial()
            .withExcluded(blockOfPlusTwoMw)
    }

    @Test
    fun `Multiple bars are be correctly identified`() {
        val graph = Graph(
            bars = mapOf(
                0 to Bar(),
                1 to Bar(),
                2 to Bar(),
                3 to Bar(),
            ),
        )

        val result = instance.analyse(graph, limitOfPlusFive)

        assertThat(result).respectsLimitAtBar(0).withoutIncluded()
        assertThat(result).respectsLimitAtBar(1).withoutIncluded()
        assertThat(result).respectsLimitAtBar(2).withoutIncluded()
        assertThat(result).respectsLimitAtBar(3).withoutIncluded()
    }
}

private class GraphLimitAnalysisAssert(actual: GraphLimitAnalysis) :
    AbstractAssert<GraphLimitAnalysisAssert, GraphLimitAnalysis>(actual, GraphLimitAnalysisAssert::class.java) {

    companion object {
        fun  assertThat(actual: GraphLimitAnalysis): GraphLimitAnalysisAssert {
            return GraphLimitAnalysisAssert(actual)
        }
    }

    fun respectsLimitAtBar(index: Int): RespectsLimitAssert {
        val bar = actual.analyses[index]
        assert(bar != null) { "Expected bar '$index', but it was not present in '$actual'" }
        assert(bar is BarRespectsLimit) { "Expected bar '$index' below limit, but it was not '$actual'" }
        return RespectsLimitAssert(bar!! as BarRespectsLimit)
    }

    fun exceedsLimitAtBar(index: Int): ExceedsLimitAssert {
        val bar = actual.analyses[index]
        assert(bar != null) { "Expected bar '$index', but it was not present in '$actual'" }
        assert(bar is BarExceedsLimit) { "Expected bar '$index' below limit, but it was not '$actual'" }
        return ExceedsLimitAssert(bar!! as BarExceedsLimit)
    }
}

private class ExceedsLimitAssert(actual: BarExceedsLimit) :
    AbstractAssert<ExceedsLimitAssert, BarExceedsLimit>(actual, ExceedsLimitAssert::class.java) {

    fun withIncluded(vararg blocks: Block): ExceedsLimitAssert {
        org.assertj.core.api.Assertions.assertThat(actual.included).isEqualTo(Bar(blocks.toList()))
        return this
    }

    fun withoutIncluded(): ExceedsLimitAssert {
        org.assertj.core.api.Assertions.assertThat(actual.included).isEqualTo(Bar(emptyList()))
        return this
    }

    fun withPartial(block: Block, value: BigDecimal): ExceedsLimitAssert {
        org.assertj.core.api.Assertions.assertThat(actual.partiallyExcluded).isEqualTo(block to value)
        return this
    }

    fun withoutPartial(): ExceedsLimitAssert {
        org.assertj.core.api.Assertions.assertThat(actual.partiallyExcluded).isNull()
        return this
    }

    fun withExcluded(vararg blocks: Block): ExceedsLimitAssert {
        org.assertj.core.api.Assertions.assertThat(actual.excluded).isEqualTo(Bar(blocks.toList()))
        return this
    }

    fun withoutExcluded(): ExceedsLimitAssert {
        org.assertj.core.api.Assertions.assertThat(actual.excluded).isEqualTo(Bar())
        return this
    }
}

private class RespectsLimitAssert(actual: BarRespectsLimit) :
    AbstractAssert<RespectsLimitAssert, BarRespectsLimit>(actual, RespectsLimitAssert::class.java) {

    fun withIncluded(vararg blocks: Block): RespectsLimitAssert {
        org.assertj.core.api.Assertions.assertThat(actual.included).isEqualTo(Bar(blocks.toList()))
        return this
    }

    fun withoutIncluded(): RespectsLimitAssert {
        org.assertj.core.api.Assertions.assertThat(actual.included).isEqualTo(Bar(emptyList()))
        return this
    }
}

private fun someGraph(vararg blocks: Block): Graph =
    Graph(
        bars = mapOf(
            0 to Bar(blocks.toList()),
        ),
    )

private data class DefaultBlock(
    override val power: BigDecimal,
    val context: String,
) : Block
