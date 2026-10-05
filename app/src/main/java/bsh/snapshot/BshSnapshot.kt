package bsh.snapshot

import bsh.Node
import java.io.Serializable

public class BshSnapshot : Serializable {
    @JvmField
    public val nodes: Array<Node>?

    private val formatVersion: Int = FORMAT_VERSION

    public constructor(nodes: Array<Node>?) {
        this.nodes = nodes
    }

    public fun getFormatVersion(): Int = formatVersion

    public companion object {
        public const val FORMAT_VERSION: Int = 1

        @JvmField
        public val serialVersionUID: Long = 1L
    }
}