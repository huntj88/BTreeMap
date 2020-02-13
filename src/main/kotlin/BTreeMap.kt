import BTreeMap.Entry

class BTreeMap<Key : Comparable<Key>, Value> {

    private val rootNode = Node<Key, Value>(isRootNode = true)

    fun get(key: Key): Value? {
        return rootNode.get(key)
    }

    fun put(key: Key, value: Value) {
        val entry = Entry(key, value)
        when (val putResponse = rootNode.put(entry)) {
            is Node.PutResponse.Success -> {

            }
            is Node.PutResponse.NodeFull<Key, Value> -> {
                rootNode.insertPromoted(putResponse)
            }
        }
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
        return rootNode.toString()
    }
}

const val numEntriesInNode = 2
const val numChildren = numEntriesInNode + 1

class Node<Key : Comparable<Key>, Value>(val isRootNode: Boolean = false) {
    val entries: Array<Entry<Key, Value>?> = Array(numEntriesInNode) { null }
    val children: Array<Node<Key, Value>?> = Array(numChildren) { null }

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
            !hasChildren && entries.last() != null -> {
                splitAndPromoteLeaf(entry)
            }
            hasChildren -> {
                when (val location = getLocationOfValue(entry.key)) {
                    is LocationOfValue.Value -> {
                        entries[location.index] = entry
                        TODO()
                    }
                    is LocationOfValue.Child -> {
                        val childNode = children[location.index]!!
                        return when (val putResponse = childNode.put(entry)) {
                            is PutResponse.Success -> putResponse
                            is PutResponse.NodeFull -> {
                                insertPromoted(putResponse)
                            }
                        }
                    }
                }
            }
            else -> {
                TODO()
            }
        }
    }

    fun splitAndPromoteLeaf(additionalEntry: Entry<Key, Value>): PutResponse.NodeFull<Key, Value> {
        this.entries.forEach { assert(it != null) }
        val sorted = arrayOf(additionalEntry, *entries).apply { sort() } //as Array<KeyValue<K, V>>

        val middle = sorted[sorted.size / 2]
        val left = Node<Key, Value>().also { child ->
            (0 until sorted.size / 2).forEach {
                child.entries[it] = sorted[it]
            }
        }

        val right = Node<Key, Value>().also { child ->
            val offset = (numEntriesInNode / 2) + 1
            (offset until sorted.size).forEach {
                child.entries[it - offset] = sorted[it]
            }
        }

        return PutResponse.NodeFull(middle!!, left, right)
    }

    fun insertPromoted(putResponse: PutResponse.NodeFull<Key, Value>): PutResponse<Key, Value> {
        fun isFull() = (entries.indexOfFirst { it == null } == -1)

        if (isFull()) {
            // this node is also full, propagate up to parent

            val valuesToRemove = arrayOf(putResponse.promoted as Entry<Key, Value>?) +
                    putResponse.left.entries +
                    putResponse.right.entries

            entries.forEachIndexed { index, entry ->
                if (entry!! in valuesToRemove) {
                    entries[index] = null
                }
            }

            return when (isFull()) {
                true -> {
                    when {
                        isRootNode && putResponse.promoted < entries[0]!! -> {
                            // root left
                            val oldRightSide = Node<Key, Value>().also {
                                it.entries[0] = entries[1]
                                it.children[0] = children[1]
                                it.children[1] = children[2]
                            }

                            val newLeftSide = Node<Key, Value>().also {
                                it.entries[0] = putResponse.promoted
                                it.children[0] = putResponse.left
                                it.children[1] = putResponse.right
                            }

                            entries[1] = null
                            children[0] = newLeftSide
                            children[1] = oldRightSide
                            children[2] = null

                            PutResponse.Success
                        }
                        putResponse.promoted < entries[0]!! -> {
                            TODO("left")
                        }
                        isRootNode && putResponse.promoted > entries[0]!! && putResponse.promoted < entries[1]!! -> {
                            // root center
                            val newLeftNode = Node<Key, Value>().also {
                                it.entries[0] = entries[0]
                                it.children[0] = children[0]
                                it.children[1] = putResponse.left
                            }

                            val newRightNode = Node<Key, Value>().also {
                                it.entries[0] = entries[1]
                                it.children[0] = putResponse.right
                                it.children[1] = children[2]
                            }

                            entries[0] = putResponse.promoted
                            entries[1] = null
                            children[0] = newLeftNode
                            children[1] = newRightNode
                            children[2] = null

                            PutResponse.Success
                        }
                        putResponse.promoted > entries[0]!! && putResponse.promoted < entries[1]!! -> {
                            TODO("center")
                        }
                        isRootNode && putResponse.promoted > entries[1]!! -> {
                            TODO("right root")
                        }
                        putResponse.promoted > entries[1]!! -> {
                            TODO("right")
                        }
                        else -> throw IllegalStateException("should not happen")
                    }
                }
                false -> putInNodeWithEmptySpace(putResponse.promoted, putResponse.left, putResponse.right)
            }

//            TODO("todo, not enough room in this node")
        }

        assert(!isFull())

        // TODO: unnecessary?
        val indexOfWhereToGo = entries.indexOfFirst {
            it == null || it.key > putResponse.promoted.key
        }

        return when (indexOfWhereToGo) {
            -1 -> {
                // not found so put it at the end
                entries[entries.lastIndex] = putResponse.promoted
                children[entries.lastIndex] = putResponse.left
                children[entries.lastIndex + 1] = putResponse.right
                PutResponse.Success
                TODO("just want to review what state looks like here")
            }
            else -> putInNodeWithEmptySpace(putResponse.promoted, putResponse.left, putResponse.right)
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

    override fun toString(): String {
        return "\nValues: " + entries.joinToString() + " Children: [" + children.joinToString() + "]"
    }
}
