package com.bookkeepersmc.loom.test.integration

import com.bookkeepersmc.loom.test.util.GradleProjectTestTrait
import spock.lang.Specification
import spock.lang.Unroll

import static com.bookkeepersmc.loom.test.LoomTestConstants.*
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class MojangMappingsProjectTest extends Specification implements GradleProjectTestTrait {
    @Unroll
    def "build (gradle #version)"() {
        setup:
        def gradle = gradleProject(project: "mojangMappings", version: version)

        when:
        def result = gradle.run(task: "build")
        def dependenciesResult = gradle.run(task: "dependencies")

        then:
        result.task(":build").outcome == SUCCESS
        dependenciesResult.task(":dependencies").outcome == SUCCESS

        where:
        version << STANDARD_TEST_VERSIONS
    }

    @Unroll
    def "build no intermediary (gradle #version)"() {
        setup:
        def gradle = gradleProject(project: "mojangMappings", version: version)
        gradle.buildGradle << '''
			loom {
				noIntermediateMappings()
			}
		'''

        when:
        def result = gradle.run(task: "build")

        then:
        result.task(":build").outcome == SUCCESS

        where:
        version << STANDARD_TEST_VERSIONS
    }

    @Unroll
    def "mojang mappings without synthetic field names (gradle #version)"() {
        setup:
        def gradle = gradleProject(project: "minimalBase", version: version)

        gradle.buildGradle << '''
                dependencies {
                    minecraft "com.mojang:minecraft:1.18-pre5"
                    mappings loom.layered {
						officialMojangMappings {
							nameSyntheticMembers = false
						}
					}
                }
            '''

        when:
        def result = gradle.run(task: "build")

        then:
        result.task(":build").outcome == SUCCESS

        where:
        version << STANDARD_TEST_VERSIONS
    }

    @Unroll
    def "fail with wrong officialMojangMappings usage (gradle #version)"() {
        setup:
        def gradle = gradleProject(project: "minimalBase", version: version)

        gradle.buildGradle << '''
				dependencies {
					minecraft "com.mojang:minecraft:1.18.2"
					mappings loom.layered {
						// This is the wrong method to call!
						loom.officialMojangMappings()
					}
				}
			'''

        when:
        def result = gradle.run(task: "build", expectFailure: true)

        then:
        result.output.contains("Use `officialMojangMappings()` when configuring layered mappings, not the extension method `loom.officialMojangMappings()`")

        where:
        version << STANDARD_TEST_VERSIONS
    }
}
