package com.athaydes.osgiaas.kotlin

import org.junit.Assert.assertEquals
import org.junit.Test
import javax.script.ScriptException

val interpreter = KotlinInterpreter()

class KotlinInterpreterTest {
    @Test
    fun canRunSimpleScript() {
        assertEquals("2", interpreter.eval("2"))
        assertEquals("4", interpreter.eval("2 + 2"))
        assertEquals("hello", interpreter.eval("\"hello\""))
    }

    @Test
    fun nothingReturnedIfNoText() {
        assertEquals(null, interpreter.eval(""))
    }

    @Test(expected = ScriptException::class)
    fun errorIfScriptDoesNotCompile() {
        interpreter.eval("this is not valid")
    }

    @Test
    fun canRunLargeScript() {
        val script = """
        import com.athaydes.osgiaas.kotlin.TestState

        fun sum(a: Int, b: Int) = a + b

        fun mult(a: Int, b: Int) = a * b

        sum(TestState.a, mult(4, 3))
        """

        assertEquals("14", interpreter.eval(script))
    }
}

object TestState {
    var a: Int = 2
}
