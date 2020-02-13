import org.junit.Test
import me.jameshunt.btree.BTreeMap
import kotlin.random.Random

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
            put(0, 7)
            put(-1, 7)
            put(-2, 7)
            put(-3, 7)
            put(-4, 7)
            put(-5, 7)
            put(-6, 7)
            put(-7, 7)
            println(this)
            println()

            put(100, 9)
            put(90, 9)
//            put(29, 9)
//            put(79, 9)

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

            println(this)
        }
    }

    @Test
    fun testABunch() {
        val range = (0..9999).toList()

        BTreeMap<Int, Int>().apply {
            range.sortedBy { Random.nextDouble() }.forEach {
                put(it, it)
            }

            range.forEach {
                val value = get(it) ?: throw IllegalStateException()
                println(value)
            }

            println(this)
        }
    }
}
