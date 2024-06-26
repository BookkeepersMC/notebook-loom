/*
 * This file is part of notebook-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022 BookkeepersMC
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.bookkeepersmc.loom.util.ZipUtils;
import com.bookkeepersmc.loom.util.gradle.SourceSetHelper;
import org.gradle.api.tasks.SourceSet;


import org.jetbrains.annotations.NotNull;

/**
 * A mod may be a zip, directory or Gradle {@link SourceSet}
 * This abstraction allows easily reading a contained file from the mod.
 */
public interface ModJsonSource {
	byte[] read(@NotNull String path) throws IOException;

	record ZipSource(Path zipPath) implements ModJsonSource {
		@Override
		public byte[] read(@NotNull String path) throws IOException {
			return ZipUtils.unpack(zipPath, path);
		}
	}

	record DirectorySource(Path directoryPath) implements ModJsonSource {
		@Override
		public byte[] read(@NotNull String path) throws IOException {
			return Files.readAllBytes(directoryPath.resolve(path));
		}
	}

	record SourceSetSource(SourceSet... sourceSets) implements ModJsonSource {
		@Override
		public byte[] read(@NotNull String path) throws IOException {
			return Files.readAllBytes(findFile(path).toPath());
		}

		private File findFile(String path) throws IOException {
			final File file = SourceSetHelper.findFirstFileInResource(path, sourceSets);

			if (file == null) {
				throw new FileNotFoundException("Could not find: " + path);
			}

			return file;
		}
	}
}
