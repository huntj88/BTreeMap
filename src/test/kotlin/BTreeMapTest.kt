import org.junit.Test

class BTreeMapTest {

    @Test
    fun get() {
        BTreeMap<Int, Int>().apply {
            put(2, 2)
            put(8, 8)
            put(6, 6)
            put(1, 1)
            put(5, 5)
            put(4, 4)
            put(9, 9)
            println(this)
            println()
            put(3, 3)
            put(10, 10)
            println(this)
            println()
            put(7, 7)
            println(this)
            println()

            println(get(1))
            println(get(2))
            println(get(3))
            println(get(4))
            println(get(5))
            println(get(6))
            println(get(7))
            println(get(8))
            println(get(9))
            println(get(10))
        }
    }

    @Test
    fun put() {
    }
}
