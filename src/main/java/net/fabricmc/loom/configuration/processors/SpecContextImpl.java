/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022-2023 FabricMC
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

package net.fabricmc.loom.configuration.processors;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.attributes.Usage;
import org.gradle.api.plugins.JavaPlugin;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.api.RemapConfigurationSettings;
import net.fabricmc.loom.api.processor.SpecContext;
import net.fabricmc.loom.util.Constants;
import net.fabricmc.loom.util.nmj.NotebookModJson;
import net.fabricmc.loom.util.nmj.NotebookModJsonFactory;
import net.fabricmc.loom.util.nmj.NotebookModJsonHelpers;
import net.fabricmc.loom.util.gradle.GradleUtils;

/**
 * @param modDependencies External mods that are depended on
 * @param localMods Mods found in the current project.
 * @param compileRuntimeMods Dependent mods found in both the compile and runtime classpath.
 */
public record SpecContextImpl(List<NotebookModJson> modDependencies, List<NotebookModJson> localMods, List<NotebookModJson> compileRuntimeMods) implements SpecContext {
	public static SpecContextImpl create(Project project) {
		final Map<String, List<NotebookModJson>> fmjCache = new HashMap<>();
		return new SpecContextImpl(getDependentMods(project, fmjCache), NotebookModJsonHelpers.getModsInProject(project), getCompileRuntimeMods(project, fmjCache));
	}

	// Reruns a list of mods found on both the compile and/or runtime classpaths
	private static List<NotebookModJson> getDependentMods(Project project, Map<String, List<NotebookModJson>> fmjCache) {
		final LoomGradleExtension extension = LoomGradleExtension.get(project);
		var mods = new ArrayList<NotebookModJson>();

		for (RemapConfigurationSettings entry : extension.getRemapConfigurations()) {
			final Set<File> artifacts = entry.getSourceConfiguration().get().resolve();

			for (File artifact : artifacts) {
				final List<NotebookModJson> notebookModJson = fmjCache.computeIfAbsent(artifact.toPath().toAbsolutePath().toString(), $ -> {
					return NotebookModJsonFactory.createFromZipOptional(artifact.toPath())
							.map(List::of)
							.orElseGet(List::of);
				});

				if (!notebookModJson.isEmpty()) {
					mods.add(notebookModJson.get(0));
				}
			}
		}

		if (!GradleUtils.getBooleanProperty(project, Constants.Properties.DISABLE_PROJECT_DEPENDENT_MODS)) {
			// Add all the dependent projects
			for (Project dependentProject : getDependentProjects(project).toList()) {
				mods.addAll(fmjCache.computeIfAbsent(dependentProject.getPath(), $ -> {
					return NotebookModJsonHelpers.getModsInProject(dependentProject);
				}));
			}
		}

		return sorted(mods);
	}

	private static Stream<Project> getDependentProjects(Project project) {
		final Stream<Project> runtimeProjects = getLoomProjectDependencies(project.getConfigurations().getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME));
		final Stream<Project> compileProjects = getLoomProjectDependencies(project.getConfigurations().getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME));

		return Stream.concat(runtimeProjects, compileProjects)
				.distinct();
	}

	// Returns a list of mods that are on both to compile and runtime classpath
	private static List<NotebookModJson> getCompileRuntimeMods(Project project, Map<String, List<NotebookModJson>> fmjCache) {
		var mods = new ArrayList<>(getCompileRuntimeModsFromRemapConfigs(project, fmjCache).toList());

		for (Project dependentProject : getCompileRuntimeProjectDependencies(project).toList()) {
			mods.addAll(fmjCache.computeIfAbsent(dependentProject.getPath(), $ -> {
				return NotebookModJsonHelpers.getModsInProject(dependentProject);
			}));
		}

		return Collections.unmodifiableList(mods);
	}

	// Returns a list of jar mods that are found on the compile and runtime remapping configurations
	private static Stream<NotebookModJson> getCompileRuntimeModsFromRemapConfigs(Project project, Map<String, List<NotebookModJson>> fmjCache) {
		final LoomGradleExtension extension = LoomGradleExtension.get(project);
		final List<Path> runtimeEntries = extension.getRuntimeRemapConfigurations().stream()
				.filter(settings -> settings.getApplyDependencyTransforms().get())
				.flatMap(resolveArtifacts(project, true))
				.toList();

		return extension.getCompileRemapConfigurations().stream()
				.filter(settings -> settings.getApplyDependencyTransforms().get())
				.flatMap(resolveArtifacts(project, false))
				.filter(runtimeEntries::contains) // Use the intersection of the two configurations.
				.map(zipPath -> {
					final List<NotebookModJson> list = fmjCache.computeIfAbsent(zipPath.toAbsolutePath().toString(), $ -> {
						return NotebookModJsonFactory.createFromZipOptional(zipPath)
								.map(List::of)
								.orElseGet(List::of);
					});
					return list.isEmpty() ? null : list.get(0);
				})
				.filter(Objects::nonNull)
				.sorted(Comparator.comparing(NotebookModJson::getId));
	}

	private static Function<RemapConfigurationSettings, Stream<Path>> resolveArtifacts(Project project, boolean runtime) {
		final Usage usage = project.getObjects().named(Usage.class, runtime ? Usage.JAVA_RUNTIME : Usage.JAVA_API);

		return settings -> {
			final Configuration configuration = settings.getSourceConfiguration().get().copyRecursive();
			configuration.setCanBeConsumed(false);
			configuration.attributes(attributes -> attributes.attribute(Usage.USAGE_ATTRIBUTE, usage));
			return configuration.resolve().stream().map(File::toPath);
		};
	}

	// Returns a list of Loom Projects found in both the runtime and compile classpath
	private static Stream<Project> getCompileRuntimeProjectDependencies(Project project) {
		final Stream<Project> runtimeProjects = getLoomProjectDependencies(project.getConfigurations().getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME));
		final List<Project> compileProjects = getLoomProjectDependencies(project.getConfigurations().getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME)).toList();

		return runtimeProjects
				.filter(compileProjects::contains); // Use the intersection of the two configurations.
	}

	// Returns a list of Loom Projects found in the provided Configuration
	private static Stream<Project> getLoomProjectDependencies(Configuration configuration) {
		return configuration.getAllDependencies()
				.withType(ProjectDependency.class)
				.stream()
				.map(ProjectDependency::getDependencyProject)
				.filter(GradleUtils::isLoomProject);
	}

	// Sort to ensure stable caching
	private static List<NotebookModJson> sorted(List<NotebookModJson> mods) {
		return mods.stream().sorted(Comparator.comparing(NotebookModJson::getId)).toList();
	}

	@Override
	public List<NotebookModJson> modDependenciesCompileRuntime() {
		return compileRuntimeMods;
	}
}
