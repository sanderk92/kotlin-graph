package graph

import java.math.BigDecimal

data class Graph(val bars: Map<Int, Bar> = emptyMap())

data class Bar(val blocks: List<Block> = emptyList()) {
    val power: BigDecimal get() = blocks.sumOf(Block::power)

    fun isEmpty(): Boolean = blocks.isEmpty()
    fun isNotEmpty(): Boolean = blocks.isNotEmpty()

    operator fun plus(block: Block): Bar = Bar(blocks + block)
    operator fun minus(block: Block): Bar = Bar(blocks - block)
    operator fun plus(bar: Bar): Bar = Bar(blocks + bar.blocks)
    operator fun minus(bar: Bar): Bar = Bar(blocks - bar.blocks.toSet())
}

interface Block {
    val power: BigDecimal
}
