package com.bookkeepersmc.loom.test.unit.service

import net.fabricmc.loom.util.gradle.GradleTypeAdapter
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import spock.lang.IgnoreIf
import spock.lang.Specification;

class GradleTypeAdapterTest extends Specification {
    def "Property"() {
        given:
        def property = Mock(Property)

        when:
        def json = GradleTypeAdapter.GSON.toJson(property)

        then:
        1 * property.get() >> "value"
        json == "\"value\""
    }

    @IgnoreIf({ os.windows })
    def "FileCollection"() {
        given:
        def file1 = new File("file1")
        def file2 = new File("file2")
        def fileCollection = Mock(FileCollection)

        when:
        def json = GradleTypeAdapter.GSON.toJson(fileCollection)

        then:
        1 * fileCollection.getFiles() >> [file1, file2].shuffled()
        json == "[\"${file1.getAbsolutePath()}\",\"${file2.getAbsolutePath()}\"]"
    }

    @IgnoreIf({ os.windows })
    def "RegularFileProperty"() {
        given:
        def file = new File("file")
        def regularFileProperty = Mock(RegularFileProperty)
        def regularFile = Mock(RegularFile)

        when:
        def json = GradleTypeAdapter.GSON.toJson(regularFileProperty)

        then:
        1 * regularFileProperty.get() >> regularFile
        1 * regularFile.getAsFile() >> file
        json == "\"${file.getAbsolutePath()}\""
    }

    def "ListProperty"() {
        given:
        def listProperty = Mock(ListProperty)
        def list = ["value1", "value2"]

        when:
        def json = GradleTypeAdapter.GSON.toJson(listProperty)

        then:
        1 * listProperty.get() >> list
        json == "[\"value1\",\"value2\"]"
    }

    def "MapProperty"() {
        given:
        def mapProperty = Mock(MapProperty)
        def map = ["key1": "value1", "key2": "value2"]

        when:
        def json = GradleTypeAdapter.GSON.toJson(mapProperty)

        then:
        1 * mapProperty.get() >> map
        json == "{\"key1\":\"value1\",\"key2\":\"value2\"}"
    }
}
