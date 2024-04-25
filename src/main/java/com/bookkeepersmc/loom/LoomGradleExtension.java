/*
 * This file is part of notebook-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016-2022 BookkeepersMC
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

package com.bookkeepersmc.loom;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import com.bookkeepersmc.loom.api.LoomGradleExtensionAPI;
import com.bookkeepersmc.loom.api.mappings.layered.MappingsNamespace;
import com.bookkeepersmc.loom.configuration.InstallerData;
import com.bookkeepersmc.loom.configuration.LoomDependencyManager;
import com.bookkeepersmc.loom.configuration.accesswidener.AccessWidenerFile;
import com.bookkeepersmc.loom.configuration.providers.mappings.LayeredMappingsFactory;
import com.bookkeepersmc.loom.configuration.providers.mappings.MappingConfiguration;
import com.bookkeepersmc.loom.configuration.providers.minecraft.MinecraftProvider;
import com.bookkeepersmc.loom.configuration.providers.minecraft.library.LibraryProcessorManager;
import com.bookkeepersmc.loom.configuration.providers.minecraft.mapped.IntermediaryMinecraftProvider;
import com.bookkeepersmc.loom.configuration.providers.minecraft.mapped.NamedMinecraftProvider;
import com.bookkeepersmc.loom.extension.LoomFiles;

import com.bookkeepersmc.loom.extension.LoomProblemReporter;
import com.bookkeepersmc.loom.extension.MixinExtension;
import com.bookkeepersmc.loom.extension.RemapperExtensionHolder;
import com.bookkeepersmc.loom.util.download.DownloadBuilder;

import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.ListProperty;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface LoomGradleExtension extends LoomGradleExtensionAPI {
	static LoomGradleExtension get(Project project) {
		return (LoomGradleExtension) project.getExtensions().getByName("loom");
	}

	LoomFiles getFiles();

	ConfigurableFileCollection getUnmappedModCollection();

	void setInstallerData(InstallerData data);

	InstallerData getInstallerData();

	void setDependencyManager(LoomDependencyManager dependencyManager);

	LoomDependencyManager getDependencyManager();

	MinecraftProvider getMinecraftProvider();

	void setMinecraftProvider(MinecraftProvider minecraftProvider);

	MappingConfiguration getMappingConfiguration();

	void setMappingConfiguration(MappingConfiguration mappingConfiguration);

	NamedMinecraftProvider<?> getNamedMinecraftProvider();

	IntermediaryMinecraftProvider<?> getIntermediaryMinecraftProvider();

	void setNamedMinecraftProvider(NamedMinecraftProvider<?> namedMinecraftProvider);

	void setIntermediaryMinecraftProvider(IntermediaryMinecraftProvider<?> intermediaryMinecraftProvider);

	default List<Path> getMinecraftJars(MappingsNamespace mappingsNamespace) {
		return switch (mappingsNamespace) {
		case NAMED -> getNamedMinecraftProvider().getMinecraftJarPaths();
		case INTERMEDIARY -> getIntermediaryMinecraftProvider().getMinecraftJarPaths();
		case OFFICIAL, CLIENT_OFFICIAL, SERVER_OFFICIAL -> getMinecraftProvider().getMinecraftJars();
		};
	}

	FileCollection getMinecraftJarsCollection(MappingsNamespace mappingsNamespace);

	boolean isRootProject();

	@Override
	MixinExtension getMixin();

	List<AccessWidenerFile> getTransitiveAccessWideners();

	void addTransitiveAccessWideners(List<AccessWidenerFile> accessWidenerFiles);

	DownloadBuilder download(String url);

	boolean refreshDeps();

	void setRefreshDeps(boolean refreshDeps);

	/**
	 * If true, multi-project optimisation mode is enabled. This mode makes builds with many Loom projects
	 * much faster by increasing sharing and disabling some functionality.
	 *
	 * <p>You can enable it by setting the Gradle property {@code fabric.loom.multiProjectOptimisation} to {@code true}.
	 */
	boolean multiProjectOptimisation();

	ListProperty<LibraryProcessorManager.LibraryProcessorFactory> getLibraryProcessors();

	ListProperty<RemapperExtensionHolder> getRemapperExtensions();

	Collection<LayeredMappingsFactory> getLayeredMappingFactories();

	LoomProblemReporter getProblemReporter();
}
