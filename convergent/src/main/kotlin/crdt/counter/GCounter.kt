package crdt.counter

import kotlin.math.max

/**
 * Grow-only counter
 * Provides state-based grow-only counter for async computation.
 */
class GCounter<T, R>(val id: T, val replicaId: R, initialValue: Long = 0) {

    @Volatile
    private var replicaValue = initialValue
    private var staleVectorClock: Map<R, Long> = HashMap()

    val value: Long
        get() {
            return getCurrentVectorClock().values.sum()
        }

    fun inc(value: Long = 1) {
        replicaValue += value
    }

    fun merge(other: GCounter<T, R>):Boolean {
        if (this.id != other.id || this.replicaId == other.replicaId) {
            return false
        }
        val thisState = this.getCurrentVectorClock()
        val otherState = other.getCurrentVectorClock()
        val vectorClock = HashMap<R, Long>()
        (thisState.keys + otherState.keys).forEach { key ->
            vectorClock[key] = max(thisState[key] ?: 0, otherState[key] ?: 0)
        }
        this.staleVectorClock = vectorClock
        return true
    }

    private fun getCurrentVectorClock(): MutableMap<R, Long> {
        val copy = staleVectorClock.toMutableMap()
        copy[replicaId] = replicaValue
        return copy
    }

}
