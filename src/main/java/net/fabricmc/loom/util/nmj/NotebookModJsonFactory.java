/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022 FabricMC
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

package net.fabricmc.loom.util.nmj;

import static net.fabricmc.loom.util.nmj.NotebookModJsonUtils.readInt;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.gradle.api.tasks.SourceSet;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.loom.LoomGradlePlugin;
import net.fabricmc.loom.util.FileSystemUtil;
import net.fabricmc.loom.util.ZipUtils;
import net.fabricmc.loom.util.gradle.SourceSetHelper;

public final class NotebookModJsonFactory {
	public static final String NOTEBOOK_MOD_JSON = "notebook.mod.json";

	private static final Logger LOGGER = LoggerFactory.getLogger(NotebookModJsonFactory.class);

	private NotebookModJsonFactory() {
	}

	@VisibleForTesting
	public static NotebookModJson create(JsonObject jsonObject, NotebookModJsonSource source) {
		int schemaVersion = 0;

		if (jsonObject.has("schemaVersion")) {
			// V0 had no schemaVersion key.
			schemaVersion = readInt(jsonObject, "schemaVersion");
		}

		return switch (schemaVersion) {
		case 0 -> new NotebookModJsonV0(jsonObject, source);
		case 1 -> new NotebookModJsonV1(jsonObject, source);
		case 2 -> new NotebookModJsonV2(jsonObject, source);
		default -> throw new UnsupportedOperationException(String.format("This version of fabric-loom doesn't support the newer fabric.mod.json schema version of (%s) Please update fabric-loom to be able to read this.", schemaVersion));
		};
	}

	public static NotebookModJson createFromZip(Path zipPath) {
		try {
			return create(ZipUtils.unpackGson(zipPath, NOTEBOOK_MOD_JSON, JsonObject.class), new NotebookModJsonSource.ZipSource(zipPath));
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to read fabric.mod.json file in zip: " + zipPath, e);
		} catch (JsonSyntaxException e) {
			throw new JsonSyntaxException("Failed to parse fabric.mod.json in zip: " + zipPath, e);
		}
	}

	@Nullable
	public static NotebookModJson createFromZipNullable(Path zipPath) {
		JsonObject jsonObject;

		try {
			jsonObject = ZipUtils.unpackGsonNullable(zipPath, NOTEBOOK_MOD_JSON, JsonObject.class);
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to read zip: " + zipPath, e);
		} catch (JsonSyntaxException e) {
			throw new JsonSyntaxException("Failed to parse fabric.mod.json in zip: " + zipPath, e);
		}

		if (jsonObject == null) {
			return null;
		}

		return create(jsonObject, new NotebookModJsonSource.ZipSource(zipPath));
	}

	public static Optional<NotebookModJson> createFromZipOptional(Path zipPath) {
		return Optional.ofNullable(createFromZipNullable(zipPath));
	}

	@Nullable
	public static NotebookModJson createFromSourceSetsNullable(SourceSet... sourceSets) throws IOException {
		final File file = SourceSetHelper.findFirstFileInResource(NOTEBOOK_MOD_JSON, sourceSets);

		if (file == null) {
			return null;
		}

		try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
			return create(LoomGradlePlugin.GSON.fromJson(reader, JsonObject.class), new NotebookModJsonSource.SourceSetSource(sourceSets));
		} catch (JsonSyntaxException e) {
			LOGGER.warn("Failed to parse fabric.mod.json: {}", file.getAbsolutePath());
			return null;
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to read " + file.getAbsolutePath(), e);
		}
	}

	public static boolean isModJar(File file) {
		return isModJar(file.toPath());
	}

	public static boolean isModJar(Path input) {
		return ZipUtils.contains(input, NOTEBOOK_MOD_JSON) || ZipUtils.contains(input, "fabric.mod.json");
	}

	public static boolean containsMod(FileSystemUtil.Delegate fs) {
		return Files.exists(fs.getPath(NOTEBOOK_MOD_JSON)) || Files.exists(fs.getPath("fabric.mod.json"));
	}
}
