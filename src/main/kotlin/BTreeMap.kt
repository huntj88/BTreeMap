import BTreeMap.Entry

class BTreeMap<Key : Comparable<Key>, Value> {

    private val rootNode = Node<Key, Value>()

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
}

const val numEntriesInNode = 2
const val numChildren = numEntriesInNode + 1

class Node<Key : Comparable<Key>, Value> {
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
                        return when(val putResponse = childNode.put(entry)) {
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

        (entries.indices).forEach {
            entries[it] = null
        }

        return PutResponse.NodeFull(middle!!, left, right)
    }

    fun insertPromoted(putResponse: PutResponse.NodeFull<Key, Value>): PutResponse<Key, Value> {
        val isFull = entries.indexOfFirst { it == null } == -1

        if(isFull) {
            // this node is also full, propagate up to parent
            TODO("todo, not enough room in this node")
        }

        assert(!isFull)

        val indexOfWhereToGo = entries.indexOfFirst {
            it == null || it.key > putResponse.promoted.key
        }

        return when(indexOfWhereToGo) {
            -1 -> {
                // not found so put it at the end
                entries[entries.lastIndex] = putResponse.promoted
                children[entries.lastIndex] = putResponse.left
                children[entries.lastIndex + 1] = putResponse.right
                PutResponse.Success
            }
            else -> putInNodeWithEmptySpace(putResponse.promoted, putResponse.left, putResponse.right)
        }
    }

    private fun putInNodeWithEmptySpace(
        newEntry: Entry<Key, Value>,
        left: Node<Key, Value>?,
        right: Node<Key, Value>?
    ): PutResponse<Key, Value> {

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
        data class NodeFull<Key: Comparable<Key>, Value>(
            val promoted: Entry<Key, Value>,
            val left: Node<Key, Value>,
            val right: Node<Key, Value>
        ) : PutResponse<Key, Value>()
    }

    sealed class LocationOfValue {
        data class Value(val index: Int) : LocationOfValue()
        data class Child(val index: Int) : LocationOfValue()
    }
}
