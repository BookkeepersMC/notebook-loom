/*
 * This file is part of notebook-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2023 BookkeepersMC
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

package com.bookkeepersmc.loom.configuration.metadata;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.bookkeepersmc.loom.LoomGradleExtension;
import com.bookkeepersmc.loom.api.metadata.ModJson;

import com.bookkeepersmc.loom.util.FileSystemUtil;
import com.bookkeepersmc.loom.util.ZipUtils;
import com.bookkeepersmc.loom.util.gradle.SourceSetHelper;

import com.bookkeepersmc.loom.util.metadata.ModJsonFactory;

import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;

public class ModMetadataHelper {
	public static final String FABRIC_MOD_JSON = "fabric.mod.json";
	public static final String NOTEBOOK_MOD_JSON = "notebook.mod.json";
	// Returns a list of Mods found in the provided project's main or client sourcesets
	public static List<ModJson> getModsInProject(Project project) {
		final LoomGradleExtension extension = LoomGradleExtension.get(project);
		var sourceSets = new ArrayList<SourceSet>();
		sourceSets.add(SourceSetHelper.getMainSourceSet(project));

		if (extension.areEnvironmentSourceSetsSplit()) {
			sourceSets.add(SourceSetHelper.getSourceSetByName("client", project));
		}

		try {
			final ModJson modJson = ModJsonFactory.createFromSourceSetsNullable(sourceSets.toArray(SourceSet[]::new));

			if (modJson != null) {
				return List.of(modJson);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return Collections.emptyList();
	}

	public static boolean isModJar(File file) {
		return isModJar(file.toPath());
	}

	public static boolean isModJar(Path input) {
		return ZipUtils.contains(input, NOTEBOOK_MOD_JSON);
	}

	public static boolean containsMod(FileSystemUtil.Delegate fs) {
		return Files.exists(fs.getPath(NOTEBOOK_MOD_JSON));
	}

	public static boolean isNotebookMod(Path path) {
		return ZipUtils.contains(path, NOTEBOOK_MOD_JSON);
	}

	public static String getMetadataPath(Path path) {
		return isNotebookMod(path) ? NOTEBOOK_MOD_JSON : FABRIC_MOD_JSON;
	}
}
