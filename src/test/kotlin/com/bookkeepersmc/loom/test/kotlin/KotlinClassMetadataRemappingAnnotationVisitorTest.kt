package com.bookkeepersmc.loom.test.kotlin

import net.fabricmc.loom.kotlin.remapping.KotlinMetadataRemappingClassVisitor
import net.fabricmc.loom.util.TinyRemapperHelper
import net.fabricmc.mappingio.MappingReader
import net.fabricmc.mappingio.tree.MemoryMappingTree
import net.fabricmc.tinyremapper.IMappingProvider
import net.fabricmc.tinyremapper.TinyRemapper
import org.junit.jupiter.api.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceClassVisitor
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Paths

class KotlinClassMetadataRemappingAnnotationVisitorTest {

    @Test
    fun simpleTest() {
        val inputPosInChunk = getClassBytes("PosInChunk")

        val classReader = ClassReader(inputPosInChunk)

        val tinyRemapper =
            TinyRemapper.newRemapper()
                .withMappings(readMappings("PosInChunk"))
                .build()

        val inputWriter = StringWriter()
        classReader.accept(stringWriterVisitor(inputWriter), 0)

        val remappedWriter = StringWriter()
        classReader.accept(KotlinMetadataRemappingClassVisitor(tinyRemapper.environment.remapper, stringWriterVisitor(remappedWriter)), 0)

        val d2In = d2(inputWriter.toString())
        val d2Out = d2(remappedWriter.toString())

        println(d2In)
        println(d2Out)
    }

    @Test
    fun extensionTest() {
        val input = getClassBytes("TestExtensionKt")
        val classReader = ClassReader(input)

        val tinyRemapper =
            TinyRemapper.newRemapper()
                .withMappings(readMappings("TestExtensionKt"))
                .build()

        val inputWriter = StringWriter()
        classReader.accept(stringWriterVisitor(inputWriter), 0)

        val remappedWriter = StringWriter()
        classReader.accept(KotlinMetadataRemappingClassVisitor(tinyRemapper.environment.remapper, stringWriterVisitor(remappedWriter)), 0)

        val d2In = d2(inputWriter.toString())
        val d2Out = d2(remappedWriter.toString())

        println(d2In)
        println(d2Out)
    }

    private fun getClassBytes(name: String): ByteArray {
        return File("src/test/resources/classes/$name.class").readBytes()
    }

    private fun readMappings(name: String): IMappingProvider {
        val mappingTree = MemoryMappingTree()
        MappingReader.read(Paths.get("src/test/resources/mappings/$name.mappings"), mappingTree)
        return TinyRemapperHelper.create(mappingTree, "named", "intermediary", false)
    }

    private fun stringWriterVisitor(writer: StringWriter): ClassVisitor {
        return TraceClassVisitor(null, TextifierImpl(), PrintWriter(writer))
    }

    private fun d2(bytecode: String): List<String> {
        val d2Regex = Regex("d2=\\{(.*)}")
        return d2Regex.find(bytecode)!!.groupValues[1].split(",")
    }

    private class TextifierImpl : Textifier(Opcodes.ASM9)
}