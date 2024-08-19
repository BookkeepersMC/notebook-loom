package com.bookkeepersmc.loom.test.unit.service

import java.nio.file.Files
import java.nio.file.Path

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.intellij.lang.annotations.Language

import net.fabricmc.loom.task.service.NewMappingsService
import net.fabricmc.loom.task.service.SourceRemapperService
import com.bookkeepersmc.loom.test.util.GradleTestUtil
import net.fabricmc.loom.util.DeletingFileVisitor

class SourceRemapperServiceTest extends ServiceTestBase {
    def "remap sources"() {
        given:
        Path tempDirectory = Files.createTempDirectory("test")
        Path sourceDirectory = tempDirectory.resolve("source")
        Path destDirectory = tempDirectory.resolve("dst")
        Path mappings = tempDirectory.resolve("mappings.tiny")

        Files.createDirectories(sourceDirectory)
        Files.createDirectories(destDirectory)
        Files.writeString(sourceDirectory.resolve("Source.java"), SOURCE)
        Files.writeString(mappings, MAPPINGS)

        SourceRemapperService service = factory.get(new TestOptions(
                mappings: GradleTestUtil.mockProperty(
                        new MappingsServiceTest.TestOptions(
                                mappingsFile: GradleTestUtil.mockRegularFileProperty(mappings.toFile()),
                                from: GradleTestUtil.mockProperty("named"),
                                to: GradleTestUtil.mockProperty("intermediary"),
                        )
                ),
        ))

        when:
        service.remapSourcesJar(sourceDirectory, destDirectory)

        then:
        // This isn't actually remapping, as we dont have the classpath setup at all. But that's not what we're testing here.
        !Files.readString(destDirectory.resolve("Source.java")).isEmpty()

        cleanup:
        Files.walkFileTree(tempDirectory, new DeletingFileVisitor())
    }

    @Language("java")
    static String SOURCE = """
        class Source {
            public void test() {
                System.out.println("Hello");
            }
        }
    """.trim()

    // Tiny v2 mappings to rename println
    static String MAPPINGS = """
    tiny	2	0	intermediary	named
    c	Source Source
    	m	()V	println	test
    """.trim()

    static class TestOptions implements SourceRemapperService.Options {
        Property<NewMappingsService.Options> mappings
        Property<Integer> javaCompileRelease = GradleTestUtil.mockProperty(17)
        ConfigurableFileCollection classpath = GradleTestUtil.mockConfigurableFileCollection()
        Property<String> serviceClass = serviceClassProperty(SourceRemapperService.TYPE)
    }
}
