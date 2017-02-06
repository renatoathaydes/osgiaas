package com.athaydes.osgiaas.kotlin

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorBasedReporter
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoots
import org.jetbrains.kotlin.cli.jvm.repl.*
import org.jetbrains.kotlin.cli.jvm.repl.messages.DiagnosticMessageHolder
import org.jetbrains.kotlin.cli.jvm.repl.messages.ReplWriter
import org.jetbrains.kotlin.cli.jvm.repl.reader.ReplCommandReader
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.addKotlinSourceRoot
import org.jetbrains.kotlin.utils.PathUtil
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.ByteBuffer
import javax.script.ScriptException

class KotlinInterpreter {

    private val repl: ReplInterpreter

    init {
        val cc = CompilerConfiguration()
        cc.addJavaSourceRoots(PathUtil.getJdkClassesRoots())
        cc.addKotlinSourceRoot(PathUtil.getResourcePathForClass(KotlinInterpreter::class.java).absolutePath)
        cc.put(JVMConfigurationKeys.MODULE_NAME, "osgiaas-kotlin-interpreter")
        repl = ReplInterpreter(Disposer.newDisposable(), cc, replConfig)
    }

    fun eval(script: String): String? {
        val result = repl.eval(script)

        return when (result) {
            is LineResult.ValueResult -> result.valueAsString
            is LineResult.Incomplete, LineResult.UnitResult -> null
            is LineResult.Error -> throw ScriptException(result.errorText)
        }
    }

}

private object replCommandReader : ReplCommandReader {
    override fun flushHistory() {
        // nothing to do
    }

    override fun readLine(next: ReplFromTerminal.WhatNextAfterOneLine) = null
}

private object replErrorLogger : ReplErrorLogger {
    override fun logException(e: Throwable): Nothing {
        e.printStackTrace()
        throw e
    }
}

private object replWriter : ReplWriter {
    override fun notifyCommandSuccess() {

    }

    override fun notifyIncomplete() {

    }

    override fun notifyReadLineEnd() {

    }

    override fun notifyReadLineStart() {

    }

    override fun outputCommandResult(x: String) {

    }

    override fun outputCompileError(x: String) {

    }

    override fun outputRuntimeError(x: String) {

    }

    override fun printlnHelpMessage(x: String) {

    }

    override fun printlnWelcomeMessage(x: String) {

    }

    override fun sendInternalErrorReport(x: String) {

    }
}

private object terminalDiagnosticMessageHolder : MessageCollectorBasedReporter, DiagnosticMessageHolder {
    private val outputStream = ByteArrayOutputStream()
    override val messageCollector = PrintingMessageCollector(PrintStream(outputStream), MessageRenderer.WITHOUT_PATHS, false)

    override val renderedDiagnostics: String
        get() = Charsets.UTF_8.decode(ByteBuffer.wrap(outputStream.toByteArray())).toString()
}

private object replConfig : ReplConfiguration {
    override val allowIncompleteLines = false
    override val commandReader = replCommandReader
    override val errorLogger = replErrorLogger
    override val writer = replWriter

    override fun createDiagnosticHolder() = terminalDiagnosticMessageHolder

    override fun onUserCodeExecuting(isExecuting: Boolean) {

    }
}

fun main(args: Array<String>) {

    val p = PathUtil.getKotlinPathsForCompiler()

    val cc = CompilerConfiguration()
    cc.addJavaSourceRoots(PathUtil.getJdkClassesRoots())

//    val kotlinFile = Files.createTempFile("kotlin-script", ".kt").toFile()
//    kotlinFile.writeText("fun main(args:Array<String>) { println(2 + 2) }")
//    cc.addKotlinSourceRoot(kotlinFile.absolutePath)
    cc.put(JVMConfigurationKeys.MODULE_NAME, "osgiaas-kotlin-interpreter")
    val repl = ReplInterpreter(Disposer.newDisposable(), cc, replConfig)
    val result = repl.eval("2 + 2 ")

    when (result) {
        is LineResult.ValueResult -> println(result.valueAsString)
        is LineResult.Incomplete, LineResult.UnitResult -> println()
        is LineResult.Error -> println(result.errorText)
    }

//    cc.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, PrintingMessageCollector(
//            System.err, MessageRenderer.PLAIN_FULL_PATHS, true))
//
//    cc.put(JVMConfigurationKeys.MODULE_NAME, "osgiaas-kotlin")
//    //cc.add(JVMConfigurationKeys.SCRIPT_DEFINITIONS, StandardScriptDefinition.getScriptName())
//
//    val e = KotlinCoreEnvironment.createForProduction(Disposer.newDisposable(), cc, emptyList())
//
//    val ex = KotlinToJVMBytecodeCompiler.compileAndExecuteScript(cc, p, e, emptyList())
//
//    println("Exit code: $ex")
}