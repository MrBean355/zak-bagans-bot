package com.github.mrbean355.zakbot.phrases.responses

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class ResponsePoolTest {

    @Test
    internal fun testConstruction_EmptyList_ThrowsException() {
        assertThrows<IllegalArgumentException> {
            ResponsePool(emptyList())
        }
    }

    @Test
    internal fun testConstruction_NonEmptyList_DoesNotThrowException() {
        assertDoesNotThrow {
            ResponsePool(listOf("Answers"))
        }
    }

    @Test
    internal fun testTotalSize_AfterConstruction_ReturnsCorrectSize() {
        val pool = ResponsePool(listOf("a", "b", "c"))

        assertEquals(3, pool.totalSize)
    }

    @Test
    internal fun testCurrentSize_AfterConstruction_ReturnsCorrectSize() {
        val pool = ResponsePool(listOf("a", "b", "c"))

        assertEquals(3, pool.currentSize)
    }

    @Test
    internal fun testTotalSize_AfterTake_ReturnsCorrectSize() {
        val pool = ResponsePool(listOf("a", "b", "c"))

        pool.take()

        assertEquals(3, pool.totalSize)
    }

    @Test
    internal fun testCurrentSize_AfterTake_ReturnsCorrectSize() {
        val pool = ResponsePool(listOf("a", "b", "c"))

        pool.take()

        assertEquals(2, pool.currentSize)
    }

    @Test
    internal fun testTake_ReturnsRandomElement() {
        val pool = ResponsePool(listOf("a", "b", "c"))

        val element = pool.take()

        assertTrue(element == "a" || element == "b" || element == "c")
    }

    @Test
    internal fun testTake_MultipleCalls_ReturnsDifferentElements() {
        val pool = ResponsePool(listOf("a", "b", "c"))

        val e1 = pool.take()
        val e2 = pool.take()
        val e3 = pool.take()

        assertEquals(3, setOf(e1, e2, e3).size)
        assertEquals(0, pool.currentSize)
    }

    @Test
    internal fun testTake_MultipleCalls_ElementsRunOut_ResetsPool() {
        val pool = ResponsePool(listOf("a", "b", "c"))

        pool.take()
        pool.take()
        pool.take()
        val e4 = pool.take()

        assertTrue(e4 == "a" || e4 == "b" || e4 == "c")
        assertEquals(2, pool.currentSize)
        assertEquals(3, pool.totalSize)
    }
}