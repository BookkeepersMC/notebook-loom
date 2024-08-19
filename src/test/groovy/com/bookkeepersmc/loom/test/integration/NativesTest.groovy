/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024 FabricMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.bookkeepersmc.loom.test.integration

import com.bookkeepersmc.loom.test.util.GradleProjectTestTrait
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.environment.RestoreSystemProperties

import static com.bookkeepersmc.loom.test.LoomTestConstants.STANDARD_TEST_VERSIONS
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

class NativesTest extends Specification implements GradleProjectTestTrait {
	@Unroll
	// 1.18
	def "Default natives for Caves and Cliffs Part 2 (gradle #version)"() {
		setup:
		def gradle = gradleProject(project: "minimalBase", version: version)

		gradle.buildGradle << '''
                    dependencies {
                        minecraft "com.mojang:minecraft:1.18"
                        mappings loom.officialMojangMappings()
                    }
        '''

		when:
		def result = gradle.run(task: "extractNatives")
		def result2 = gradle.run(task: "extractNatives")

		then:
		result.task(":extractNatives").outcome == SUCCESS
		result2.task(":extractNatives").outcome == UP_TO_DATE

		where:
		version << STANDARD_TEST_VERSIONS
	}

	@RestoreSystemProperties
	@Unroll
	// 1.18
	def "Overridden natives for Caves and Cliffs Part 2 (gradle #version)"() {
		setup:
		System.setProperty('loom.test.lwjgloverride', "true")
		def gradle = gradleProject(project: "minimalBase", version: version)

		gradle.buildGradle << '''
                    dependencies {
                        minecraft "com.mojang:minecraft:1.18"
                        mappings loom.officialMojangMappings()
                    }
        '''

		when:
		def result = gradle.run(task: "extractNatives")

		then:
		result.task(":extractNatives").outcome == SUCCESS

		where:
		version << STANDARD_TEST_VERSIONS
	}

	@RestoreSystemProperties
	@Unroll
	def "Wild update classpath natives (gradle #version)"() {
		setup:
		def gradle = gradleProject(project: "minimalBase", version: version)

		gradle.buildGradle << '''
                loom {
                    noIntermediateMappings()
                }

                dependencies {
                    minecraft "com.mojang:minecraft:1.19-pre1"
                    mappings loom.layered() {
                        // No names
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
}
