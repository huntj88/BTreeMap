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
//                TODO("put promoted in correct spot in entries, and put children in correct spot")
//

                // todo: only provide values that make it append to end for now
                val firstOpenSpot = rootNode.entries.indexOfFirst { it == null }
                assert(firstOpenSpot != -1) // todo

                // todo: put in correct spot
                rootNode.entries[firstOpenSpot] = putResponse.promoted
                rootNode.children[firstOpenSpot] = putResponse.left
                rootNode.children[firstOpenSpot + 1] = putResponse.right
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
            !hasChildren && entries.last() == null -> putInLeafWithEmptySpace(entry)
            !hasChildren && entries.last() != null -> {
                splitAndPromote(entry)
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
                                val firstOpenSpot = entries.indexOfFirst { it == null }
                                assert(firstOpenSpot != -1) // todo, not enough room in this node


                                // todo: put in correct spot
                                entries[firstOpenSpot] = putResponse.promoted
                                children[firstOpenSpot] = putResponse.left
                                children[firstOpenSpot + 1] = putResponse.right

                                PutResponse.Success
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

    fun splitAndPromote(additionalEntry: Entry<Key, Value>): PutResponse.NodeFull<Key, Value> {
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

//        entries[0] = middle
//        children[0] = left
//        children[1] = right

        return PutResponse.NodeFull(middle!!, left, right)
    }

    private fun putInLeafWithEmptySpace(newEntry: Entry<Key, Value>): PutResponse<Key, Value> {
        entries.forEachIndexed { index, keyValue ->
            keyValue?.let { existing ->
                if (existing.key == newEntry.key) {
                    entries[index] = newEntry
                    return PutResponse.Success
                }
                if (existing.key > newEntry.key) {
                    (entries.size - 1 downTo index + 1).forEach {
                        if (entries[it - 1] == null) return@forEach
                        entries[it] = entries[it - 1]
                    }
                    entries[index] = newEntry
                    return PutResponse.Success
                }
            } ?: let {
                entries[index] = newEntry
                return PutResponse.Success
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
//        object Promote: PutResponse()
    }

    sealed class LocationOfValue {
        data class Value(val index: Int) : LocationOfValue()
        data class Child(val index: Int) : LocationOfValue()
    }
}
