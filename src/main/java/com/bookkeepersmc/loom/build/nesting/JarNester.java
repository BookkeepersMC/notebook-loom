/*
 * This file is part of notebook-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2018-2021 BookkeepersMC
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

package com.bookkeepersmc.loom.build.nesting;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bookkeepersmc.loom.configuration.metadata.ModMetadataHelper;
import com.bookkeepersmc.loom.util.Pair;
import com.bookkeepersmc.loom.util.ZipUtils;
import com.bookkeepersmc.loom.util.metadata.ModJsonFactory;
import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.gradle.api.UncheckedIOException;
import org.slf4j.Logger;


public class JarNester {
	public static void nestJars(Collection<File> jars, File modJar, Logger logger) {
		if (jars.isEmpty()) {
			logger.debug("Nothing to nest into " + modJar.getName());
			return;
		}

		Preconditions.checkArgument(ModMetadataHelper.isModJar(modJar), "Cannot nest jars into non mod jar " + modJar.getName());
		List<String> files = new ArrayList<>();

		for (File file : jars) {
			String nestedJarPath = "META-INF/jars" + file.getName();
			Preconditions.checkArgument(ModMetadataHelper.isModJar(file), "Cannot nest non mod jar: " + file.getName());
			files.add(nestedJarPath);
		}

		try {
			ZipUtils.add(modJar.toPath(), jars.stream().map(file -> {
				try {
					return new Pair<>("META-INF/jars/" + file.getName(), Files.readAllBytes(file.toPath()));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}).collect(Collectors.toList()));
			int count = ZipUtils.transformJson(JsonObject.class, modJar.toPath(), Stream.of(new Pair<>(ModMetadataHelper.getMetadataPath(modJar.toPath()), json -> ModJsonFactory.createFromZip(modJar.toPath()).addNestedJars(json, files))));

			Preconditions.checkState(count > 0, "Failed to transform mod metadata file");
		} catch (IOException e) {
			throw new java.io.UncheckedIOException("Failed to nest jars into " + modJar.getName(), e);
		}
	}
}
