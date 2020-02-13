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
        }
    }

    @Test
    fun test1() {
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
            put(3,3)
//            put(-1,3)
            println(this)
        }
    }

    @Test
    fun test2() {
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
            put(0,0)
            put(-1,-1)
            put(-2,-1)
            put(-3,-1)
            put(-4,-1)
//            put(-5,-1)
            println(this)
        }
    }

    @Test
    fun test3() {
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
            put(10,10)
            println(this)
        }
    }
}
