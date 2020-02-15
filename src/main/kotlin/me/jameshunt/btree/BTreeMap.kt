package me.jameshunt.btree

import me.jameshunt.btree.BTreeMap.Entry

class BTreeMap<Key : Comparable<Key>, Value>(numEntriesInNode: Int = 12) {

    init {
        assert(numEntriesInNode % 2 == 0) { "Even numbers for numEntriesInNode only" }
    }

    private val rootNode = Node<Key, Value>(numEntriesInNode)

    fun get(key: Key): Value? {
        return rootNode.get(key)
    }

    fun put(key: Key, value: Value) {
        val entry = Entry(key, value)

        val putResponse = rootNode.put(entry)
        if (putResponse is Node.PutResponse.NodeFull<Key, Value>) {
            rootNode.createNewTopLevel(putResponse)
        }
    }

    private fun Node<Key, Value>.createNewTopLevel(putResponse: Node.PutResponse.NodeFull<Key, Value>) {
        entries[0] = putResponse.promoted
        (1..entries.lastIndex).forEach { entries[it] = null }

        children[0] = putResponse.left
        children[1] = putResponse.right
        (2..children.lastIndex).forEach { children[it] = null }
    }

    data class Entry<Key : Comparable<Key>, Value>(
        override val key: Key,
        override val value: Value
    ) : Map.Entry<Key, Value>, Comparable<Entry<Key, Value>> {
        override fun compareTo(other: Entry<Key, Value>): Int {
            return key.compareTo(other.key)
        }
    }

    override fun toString(): String {
        return rootNode.toString(0)
    }
}

class Node<Key : Comparable<Key>, Value>(private val numEntriesInNode: Int) {
    val entries: Array<Entry<Key, Value>?> = Array(numEntriesInNode) { null }
    val children: Array<Node<Key, Value>?> = Array(numEntriesInNode + 1) { null }

    fun get(key: Key): Value? {
        return when (val location = getLocationOfValue(key)) {
            is LocationOfValue.Value -> entries[location.index]?.value
            is LocationOfValue.Child -> children[location.index]?.get(key)
        }
    }

    fun put(entry: Entry<Key, Value>): PutResponse<Key, Value> {
        val hasChildren = hasChildren()

        return when {
            !hasChildren && entries.last() == null -> putInNodeWithEmptySpace(entry, null, null)
            !hasChildren && entries.last() != null -> splitAndPromoteLeaf(entry)
            hasChildren -> {
                when (val location = getLocationOfValue(entry.key)) {
                    is LocationOfValue.Value -> {
                        entries[location.index] = entry
                        PutResponse.Success
                    }
                    is LocationOfValue.Child -> {
                        val childNode = children[location.index]!!
                        return when (val putResponse = childNode.put(entry)) {
                            is PutResponse.Success -> putResponse
                            is PutResponse.NodeFull -> insertPromoted(putResponse)
                        }
                    }
                }
            }
            else -> throw IllegalStateException("Should be no unhandled cases")
        }
    }

    private fun splitAndPromoteLeaf(additionalEntry: Entry<Key, Value>): PutResponse.NodeFull<Key, Value> {
        this.entries.forEach { assert(it != null) }
        // TODO: optimize sort out
        val sorted = arrayOf(additionalEntry, *entries).apply { sort() } //as Array<KeyValue<K, V>>

        val middle = sorted[sorted.size / 2]
        val left = createNode { node ->
            (0 until sorted.size / 2).forEach {
                node.entries[it] = sorted[it]
            }
        }

        val right = createNode { node ->
            val offset = (numEntriesInNode / 2) + 1
            (offset until sorted.size).forEach {
                node.entries[it - offset] = sorted[it]
            }
        }

        return PutResponse.NodeFull(middle!!, left, right)
    }

    private fun insertPromoted(putResponse: PutResponse.NodeFull<Key, Value>): PutResponse<Key, Value> {
        fun isFull() = (entries.indexOfFirst { it == null } == -1)

        return when (isFull()) {
            true -> {
                val halfNumEntry = numEntriesInNode / 2
                when {
                    putResponse.promoted < entries[0]!! -> {
                        val oldRightSide = createNode { node ->
                            (halfNumEntry until numEntriesInNode).forEachIndexed { index, oldPosition ->
                                node.entries[index] = entries[oldPosition]
                                node.children[index] = children[oldPosition]
                            }
                            node.children[halfNumEntry] = children[numEntriesInNode]
                        }

                        val newLeftSide = createNode { node ->
                            node.entries[0] = putResponse.promoted
                            (1 until halfNumEntry).forEach {
                                node.entries[it] = entries[it - 1]
                            }

                            node.children[0] = putResponse.left
                            node.children[1] = putResponse.right

                            (1..halfNumEntry).forEach {
                                node.children[it + 1] = children[it]
                            }
                        }

                        PutResponse.NodeFull(
                            promoted = entries[halfNumEntry - 1]!!,
                            left = newLeftSide,
                            right = oldRightSide
                        )
                    }
                    putResponse.promoted > entries[0]!! && putResponse.promoted < entries.last()!! -> {
                        val indexOfPromoted = entries.indexOfFirst { it!!.key > putResponse.promoted.key }

                        val leftNode = createNode { node ->
                            (0 until indexOfPromoted).forEach {
                                node.entries[it] = entries[it]
                                node.children[it] = children[it]
                            }
                            node.children[indexOfPromoted] = putResponse.left
                        }

                        val rightNode = createNode { node ->
                            node.children[0] = putResponse.right
                            (indexOfPromoted until numEntriesInNode).forEachIndexed { index, indexOld ->
                                node.entries[index] = entries[indexOld]
                                node.children[index + 1] = children[indexOld + 1]
                            }
                        }

                        return PutResponse.NodeFull(putResponse.promoted, leftNode, rightNode)
                    }
                    putResponse.promoted > entries[entries.lastIndex]!! -> {
                        this.entries.forEach { assert(it != null) }

                        val oldLeftSide = createNode { node ->
                            (0 until halfNumEntry).forEach {
                                node.entries[it] = entries[it]
                                node.children[it] = children[it]
                            }
                            node.children[halfNumEntry] = children[halfNumEntry]
                        }

                        val newRightSide = createNode { node ->
                            val offset = halfNumEntry + 1
                            (offset until numEntriesInNode).forEach {
                                node.entries[it - offset] = entries[it]
                            }
                            node.entries[halfNumEntry - 1] = putResponse.promoted

                            val grabFromIndex = node.children.size / 2 + 1
                            (0..halfNumEntry - 2).forEach {
                                node.children[it] = children[grabFromIndex + it]
                            }
                            node.children[halfNumEntry - 1] = putResponse.left
                            node.children[halfNumEntry] = putResponse.right
                        }

                        PutResponse.NodeFull(
                            promoted = entries[halfNumEntry]!!,
                            left = oldLeftSide,
                            right = newRightSide
                        )
                    }
                    else -> throw IllegalStateException("should not happen")
                }
            }
            false -> putInNodeWithEmptySpace(putResponse.promoted, putResponse.left, putResponse.right)
        }
    }

    private fun putInNodeWithEmptySpace(
        newEntry: Entry<Key, Value>,
        left: Node<Key, Value>?,
        right: Node<Key, Value>?
    ): PutResponse<Key, Value> {

        fun getIndexOfSpotForPut(newEntry: Entry<Key, Value>): Int {
            assert(entries.last() == null)
            entries.forEachIndexed { index, keyValue ->
                when {
                    keyValue == null -> return index
                    keyValue.key == newEntry.key -> return index
                    keyValue.key > newEntry.key -> return index
                }
            }

            throw IllegalStateException("should never get here")
        }

        val index = getIndexOfSpotForPut(newEntry)
        val existingEntry = entries[index]
        return when {
            existingEntry == null -> {
                entries[index] = newEntry
                children[index] = left
                children[index + 1] = right
                PutResponse.Success
            }
            existingEntry.key == newEntry.key -> {
                entries[index] = newEntry
                children[index] = left
                children[index + 1] = right
                PutResponse.Success
            }
            existingEntry.key > newEntry.key -> {
                (entries.size - 1 downTo index + 1).forEach {
                    if (entries[it - 1] == null) return@forEach
                    entries[it] = entries[it - 1]
                    children[it + 1] = children[it]
                }
                entries[index] = newEntry
                children[index] = left
                children[index + 1] = right
                PutResponse.Success
            }
            else -> throw IllegalStateException("should never get here")
        }
    }

    private fun hasChildren(): Boolean {
        this.children.forEach {
            if (it != null) return true
        }
        return false
    }

    private fun getLocationOfValue(key: Key): LocationOfValue {
        entries.forEachIndexed { index, keyValue ->
            when {
                keyValue == null -> return LocationOfValue.Child(index)
                keyValue.key == key -> return LocationOfValue.Value(index)
                keyValue.key > key -> return LocationOfValue.Child(index)
            }
        }

        return LocationOfValue.Child(children.lastIndex)
    }

    sealed class PutResponse<out Key, out Value> {
        object Success : PutResponse<Nothing, Nothing>()
        data class NodeFull<Key : Comparable<Key>, Value>(
            val promoted: Entry<Key, Value>,
            val left: Node<Key, Value>,
            val right: Node<Key, Value>
        ) : PutResponse<Key, Value>()
    }

    sealed class LocationOfValue {
        data class Value(val index: Int) : LocationOfValue()
        data class Child(val index: Int) : LocationOfValue()
    }

    private fun createNode(config: (Node<Key, Value>) -> Unit): Node<Key, Value> {
        return Node<Key, Value>(numEntriesInNode).also { config(it) }
    }

    fun toString(indentLevel: Int): String {
        val indent = (0..indentLevel).joinToString { "\t" }
        return (0 until numEntriesInNode).fold("") { acc, next ->
            acc +
                    (children[next]?.toString(indentLevel + 1)?.let { "\n$indent$it" } ?: "") +
                    (entries[next]?.let { "\n$indent$it" } ?: "")
        } + (children[numEntriesInNode]?.toString(indentLevel + 1) ?: "")
    }
}
