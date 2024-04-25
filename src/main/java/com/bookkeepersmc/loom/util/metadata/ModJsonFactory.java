/*
 * This file is part of notebook-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024 BookkeepersMC
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

package com.bookkeepersmc.loom.util.metadata;

import com.bookkeepersmc.loom.api.metadata.ModJson;

import com.bookkeepersmc.loom.configuration.metadata.ModMetadataHelper;

import com.bookkeepersmc.loom.util.fmj.NotebookModJsonFactory;
import com.bookkeepersmc.loom.util.gradle.SourceSetHelper;

import org.gradle.api.tasks.SourceSet;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class ModJsonFactory {
	public static ModJson createFromZip(Path zipPath) {
		return NotebookModJsonFactory.createFromZip(zipPath);
	}

	public static ModJson createFromZipNullable(Path zipPath) {
			return NotebookModJsonFactory.createFromZipNullable(zipPath);
	}

	public static Optional<? extends ModJson> createFromZipOptional(Path zipPath) {
			return NotebookModJsonFactory.createFromZipOptional(zipPath);
	}

	@Nullable
	public static ModJson createFromSourceSetsNullable(SourceSet... sourceSets) throws IOException {
		File file = SourceSetHelper.findFirstFileInResource(ModMetadataHelper.NOTEBOOK_MOD_JSON, sourceSets);
		if (file != null) {
			return NotebookModJsonFactory.createFromSourceSetsNullable(sourceSets);
		}
		return null;
	}
}
