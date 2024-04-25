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

package com.bookkeepersmc.loom.util.fmj;


import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import com.bookkeepersmc.loom.LoomGradlePlugin;
import com.bookkeepersmc.loom.util.ZipUtils;
import com.bookkeepersmc.loom.util.gradle.SourceSetHelper;
import com.bookkeepersmc.loom.util.metadata.ModJsonSource;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import org.gradle.api.tasks.SourceSet;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.bookkeepersmc.loom.util.metadata.ModMetadataUtils.readInt;


public final class NotebookModJsonFactory {
	public static final String NOTEBOOK_MOD_JSON = "notebook.mod.json";

	private static final Logger LOGGER = LoggerFactory.getLogger(NotebookModJsonFactory.class);

	private NotebookModJsonFactory() {
	}

	@VisibleForTesting
	public static NotebookModJson create(JsonObject jsonObject, ModJsonSource source) {
		int schemaVersion = 0;

		if (jsonObject.has("schema_version")) {
			// V0 had no schemaVersion key.
			schemaVersion = readInt(jsonObject, "schema_version");
		}

		return switch (schemaVersion) {
		case 0 -> new NotebookModJsonV0(jsonObject, source);
		case 1 -> new NotebookModJsonV1(jsonObject, source);
		case 2 -> new NotebookModJsonV2(jsonObject, source);
		default -> throw new UnsupportedOperationException(String.format("This version of notebook-loom doesn't support the newer notebook.mod.json schema version of (%s) Please update notebook-loom to be able to read this.", schemaVersion));
		};
	}

	public static NotebookModJson createFromZip(Path zipPath) {
		try {
			return create(ZipUtils.unpackGson(zipPath, NOTEBOOK_MOD_JSON, JsonObject.class), new ModJsonSource.ZipSource(zipPath));
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to read notebook.mod.json file in zip: " + zipPath, e);
		}
	}

	@Nullable
	public static NotebookModJson createFromZipNullable(Path zipPath) {
		JsonObject jsonObject;

		try {
			jsonObject = ZipUtils.unpackGsonNullable(zipPath, NOTEBOOK_MOD_JSON, JsonObject.class);
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to read zip: " + zipPath, e);
		}

		if (jsonObject == null) {
			return null;
		}

		return create(jsonObject, new ModJsonSource.ZipSource(zipPath));
	}

	public static Optional<NotebookModJson> createFromZipOptional(Path zipPath) {
		return Optional.ofNullable(createFromZipNullable(zipPath));
	}

	public static NotebookModJson createFromDirectory(Path directory) throws IOException {
		final Path path = directory.resolve(NOTEBOOK_MOD_JSON);

		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			return create(LoomGradlePlugin.GSON.fromJson(reader, JsonObject.class), new ModJsonSource.DirectorySource(directory));
		}
	}

	@Nullable
	public static NotebookModJson createFromSourceSetsNullable(SourceSet... sourceSets) throws IOException {
		final File file = SourceSetHelper.findFirstFileInResource(NOTEBOOK_MOD_JSON, sourceSets);

		if (file == null) {
			return null;
		}

		try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
			return create(LoomGradlePlugin.GSON.fromJson(reader, JsonObject.class), new ModJsonSource.SourceSetSource(sourceSets));
		} catch (JsonSyntaxException e) {
			LOGGER.warn("Failed to parse notebook.mod.json: {}", file.getAbsolutePath());
			return null;
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to read " + file.getAbsolutePath(), e);
		}
	}
}
