/*
 * This file is part of notebook-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016-2023 BookkeepersMC
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

import java.util.List;
import java.util.Objects;

import com.bookkeepersmc.loom.api.LoomGradleExtensionAPI;
import com.bookkeepersmc.loom.bootstrap.BootstrappedPlugin;
import com.bookkeepersmc.loom.configuration.CompileConfiguration;
import com.bookkeepersmc.loom.configuration.LoomConfigurations;
import com.bookkeepersmc.loom.configuration.MavenPublication;
import com.bookkeepersmc.loom.configuration.NotebookExtension;
import com.bookkeepersmc.loom.configuration.ide.IdeConfiguration;
import com.bookkeepersmc.loom.configuration.ide.idea.IdeaConfiguration;
import com.bookkeepersmc.loom.decompilers.DecompilerConfiguration;
import com.bookkeepersmc.loom.extension.LoomFiles;
import com.bookkeepersmc.loom.extension.LoomGradleExtensionImpl;
import com.bookkeepersmc.loom.task.LoomTasks;
import com.bookkeepersmc.loom.task.RemapTaskConfiguration;
import com.bookkeepersmc.loom.util.LibraryLocationLogger;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.gradle.api.Project;
import org.gradle.api.plugins.PluginAware;

public class LoomGradlePlugin implements BootstrappedPlugin {
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	public static final String LOOM_VERSION = "1.0.2";

	/**
	 * An ordered list of setup job classes.
	 */
	private static final List<Class<? extends Runnable>> SETUP_JOBS = List.of(
			LoomConfigurations.class,
			CompileConfiguration.class,
			MavenPublication.class,
			RemapTaskConfiguration.class,
			LoomTasks.class,
			DecompilerConfiguration.class,
			IdeaConfiguration.class,
			IdeConfiguration.class
	);

	@Override
	public void apply(PluginAware target) {
		target.getPlugins().apply(LoomRepositoryPlugin.class);

		if (target instanceof Project project) {
			apply(project);
		}
	}

	public void apply(Project project) {
		project.getLogger().lifecycle("Notebook Loom: " + LOOM_VERSION);
		LibraryLocationLogger.logLibraryVersions();

		// Apply default plugins
		project.apply(ImmutableMap.of("plugin", "java-library"));
		project.apply(ImmutableMap.of("plugin", "eclipse"));
		project.apply(ImmutableMap.of("plugin", "idea"));

		// Setup extensions
		project.getExtensions().create(LoomGradleExtensionAPI.class, "loom", LoomGradleExtensionImpl.class, project, LoomFiles.create(project));
		project.getExtensions().create("notebook", NotebookExtension.class);

		for (Class<? extends Runnable> jobClass : SETUP_JOBS) {
			project.getObjects().newInstance(jobClass).run();
		}
	}
}
