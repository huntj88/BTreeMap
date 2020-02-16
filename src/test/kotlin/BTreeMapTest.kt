import me.jameshunt.btree.BTreeMap
import org.junit.BeforeClass
import org.junit.Test
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


    companion object {
        lateinit var bTree: BTreeMap<Int, Unit>

        @BeforeClass
        @JvmStatic
        fun setup() {
            val random = Random(1)
            bTree = BTreeMap<Int, Unit>(200).apply {
                (0..10_000_000).sortedBy { random.nextInt() }.forEach { put(it, Unit) }
            }
        }
    }

    @Test
    fun getAllTest() {
        val range = (1..60000)
        val random = Random(1)

        BTreeMap<Int, Int>().apply {
            val nums = range.sortedBy { random.nextDouble() }

            nums.forEach { put(it, it) }

            range.forEach {
                get(it) ?: throw IllegalStateException()
            }

            println(this)
        }
    }


    @Test
    fun getSpecificFrom10Million() {
        bTree.get(4343) ?: throw IllegalStateException()
        bTree.get(234233) ?: throw IllegalStateException()
        bTree.get(577432) ?: throw IllegalStateException()
        bTree.get(4368743) ?: throw IllegalStateException()
        bTree.get(9368743) ?: throw IllegalStateException()
        bTree.get(10_000_001)?.let { throw IllegalStateException("should be null") }
    }
}
