package com.bookkeepersmc.loom.test.integration

import com.bookkeepersmc.loom.test.util.GradleProjectTestTrait
import spock.lang.Specification
import spock.lang.Unroll

import static com.bookkeepersmc.loom.test.LoomTestConstants.STANDARD_TEST_VERSIONS
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class DataGenerationTest extends Specification implements GradleProjectTestTrait {
    @Unroll
    def "dataGeneration (gradle #version)"() {
        setup:
        def gradle = gradleProject(project: "minimalBase", version: version)
        gradle.buildGradle << '''
        notebook {
            configureDataGeneration()
        }
        
        dependencies {
            minecraft "com.mojang:minecraft:1.20.2"
            mappings "net.fabricmc:yarn:1.20.2+build.4:v2"
            modImplementation "com.bookkeepersmc:notebook-loader:0.3.0"
        }
     '''
        when:
        def result = gradle.run(task: "runDatagen")

        then:
        result.task(":runDatagen").outcome == SUCCESS

        where:
        version << STANDARD_TEST_VERSIONS
    }

    @Unroll
    def "dataGeneration sourceset (gradle #version)"() {
        setup:
        def gradle = gradleProject(project: "minimalBase", version: version)
        gradle.buildGradle << '''
                // Must configure the main mod
                loom.mods {
                    "example" {
                        sourceSet sourceSets.main
                    }
                }

                notebook {
                    configureDataGeneration {
                        createSourceSet = true
                        createRunConfiguration = true
                        modId = "example-datagen"
                        strictValidation = true
                    }
                }

                dependencies {
                    minecraft "com.mojang:minecraft:1.21.1"
                    mappings "net.fabricmc:yarn:1.21.1+build.3:v2"
                    modImplementation "com.bookkeepersmc:notebook-loader:0.3.0"

                    modDatagenImplementation notebook.module("notebook-datagen-api-v1", "1.2.1+1.21.1")
                }

                println("%%" + loom.runs.datagen.configName + "%%")
            '''
        when:
        def result = gradle.run(task: "runDatagen")

        then:
        result.task(":runDatagen").outcome == SUCCESS
        result.output.contains("%%Data Generation%%")

        where:
        version << STANDARD_TEST_VERSIONS
    }
}
