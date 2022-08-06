package crdt.counter

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


internal class GCounterTest {

    @Test
    fun `without inc() calls`() {
        val counter = GCounter<Long, Long>(1, 1)
        assertEquals(0, counter.value, "Value isn't increased by one")
    }

    @Test
    fun `inc() should increase value by one`() {
        val counter = GCounter<Long, Long>(1, 1)
        val before = counter.value
        counter.inc()
        val after = counter.value
        assertEquals(1, after - before, "Value isn't increased by one")
    }

    @Test
    fun `inc() with param should increase by param value`() {
        val counter = GCounter<Long, Long>(1, 1)
        val before = counter.value
        counter.inc(7)
        val after = counter.value
        assertEquals(7, after - before, "Value isn't increased by one")
    }

    @Test
    fun `several inc() calls should increase value by calls count`() {
        val counter = GCounter<Long, Long>(1, 1)
        for (i in 1..10) {
            counter.inc()
        }
        assertEquals(10, counter.value, "Value isn't increased by ten")
    }

    @Test
    fun `merge() with another unused GCounter should not change value`() {
        val counter = GCounter<Long, Long>(1, 1)
        for (i in 1..10) {
            counter.inc()
        }
        val another = GCounter<Long, Long>(1, 2)
        val before = counter.value
        val success = counter.merge(another)
        assertTrue(success, "Merge is failed")
        assertEquals(before, counter.value, "Value is changed")
    }

    @Test
    fun `merge() with another used GCounter should change value`() {
        val counter = GCounter<Long, Long>(1, 1)
        for (i in 1..10) {
            counter.inc()
        }
        val another = GCounter<Long, Long>(1, 2)
        for (i in 1..7) {
            another.inc()
        }
        val success = counter.merge(another)
        assertTrue(success, "Merge is failed")
        assertEquals(17, counter.value)
    }

    @Test
    fun `second merge() call`() {
        val firstCounter = GCounter<Long, Long>(1, 1, 10)
        val secondCounter = GCounter<Long, Long>(1, 2, 7)
        val fSuccess = firstCounter.merge(secondCounter)
        for (i in 1..5) {
            secondCounter.inc()
        }
        val sSuccess = firstCounter.merge(secondCounter)
        assertTrue(fSuccess, "First merge is failed")
        assertTrue(sSuccess, "Second merge is failed")
        assertEquals(22, firstCounter.value)
    }

    @Test
    fun `merge() should be a commutative method`() {
        val l1 = GCounter<Long, Long>(1, 1, 10)
        val r1 = GCounter<Long, Long>(1, 2, 7)
        l1.merge(r1)

        val l2 = GCounter<Long, Long>(1, 2, 7)
        val r2 = GCounter<Long, Long>(1, 1, 10)
        l2.merge(r2)

        assertEquals(l1.value, l2.value)
    }

    @Test
    fun `merge() should be an associative method`() {
        val f1 = GCounter<Long, Long>(1, 1, 10)
        val f2 = GCounter<Long, Long>(1, 2, 7)
        val f3 = GCounter<Long, Long>(1, 3, 5)
        f1.merge(f2)
        f3.merge(f1)

        val s1 = GCounter<Long, Long>(1, 1, 10)
        val s2 = GCounter<Long, Long>(1, 2, 7)
        val s3 = GCounter<Long, Long>(1, 3, 5)
        s2.merge(s3)
        s1.merge(s2)

        assertEquals(f3.value, s1.value)
    }

    @Test
    fun `merge() should be an idempotent method`() {
        val f1 = GCounter<Long, Long>(1, 1, 10)
        val f2 = GCounter<Long, Long>(1, 2, 7)
        f1.merge(f2)
        val value1 = f1.value

        val f2Copy = GCounter<Long, Long>(1, 2, 7)
        f1.merge(f2Copy)
        val value2 = f1.value

        f1.merge(f2)
        val value3 = f1.value

        assertEquals(value1, value2)
        assertEquals(value2, value3)
    }

    @Test
    fun `merge() should not be a reflexive method`() {
        val f = GCounter<Long, Long>(1, 1, 10)
        val success1 = f.merge(f)
        assertFalse(success1, "Merge with same object is success")

        val s = GCounter<Long, Long>(1, 1, 10)
        val sUpdated = GCounter<Long, Long>(1, 1, 12)
        val success2 = s.merge(sUpdated)
        assertFalse(success2, "Merge with same replica id and different value is success")

        val t = GCounter<Long, Long>(1, 1, 10)
        val tCopy = GCounter<Long, Long>(1, 1, 10)
        val success3 = t.merge(tCopy)
        assertFalse(success3, "Merge with same replica id and same value is success")
    }


    @Test
    fun `inc() and merge() should be executed asynchronously`() {
        val counter = GCounter(1, 1)
        runBlocking {
            launch {
                for (i in 1..10) {
                    delay(i * 100L)
                    counter.inc()
                }
            }
            launch {
                for (i in 2..10) {
                    delay(i * 90L)
                    val other = GCounter(1, i)
                    other.inc()
                    counter.merge(other)
                }
            }
        }
        assertEquals(19, counter.value)
    }

    @Test
    fun `async inc()`() {
        val counter = GCounter(1, 1)
        runBlocking {
            for (i in 1..10) {
                launch {
                    for (j in 1..10) {
                        delay(10L * j * (10 - j))
                        counter.inc()
                    }
                }
            }
        }
        assertEquals(100, counter.value)
    }

}
