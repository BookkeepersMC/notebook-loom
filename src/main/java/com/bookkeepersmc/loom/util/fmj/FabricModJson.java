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

package com.bookkeepersmc.loom.util.fmj;

import java.util.List;
import java.util.Objects;

import com.bookkeepersmc.loom.api.metadata.ModJson;
import com.bookkeepersmc.loom.util.Constants;
import com.bookkeepersmc.loom.util.metadata.ModJsonSource;
import com.bookkeepersmc.loom.util.metadata.ModMetadataUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

public abstract sealed class FabricModJson implements ModJson permits FabricModJsonV0, FabricModJsonV1, FabricModJsonV2, FabricModJson.Mockable {
	protected final JsonObject jsonObject;
	private final ModJsonSource source;

	protected FabricModJson(JsonObject jsonObject, ModJsonSource source) {
		this.jsonObject = Objects.requireNonNull(jsonObject);
		this.source = Objects.requireNonNull(source);
	}


	@Override
	public String getId() {
		return ModMetadataUtils.readString(jsonObject, "id");
	}

	@Override
	public String getModVersion() {
		return ModMetadataUtils.readString(jsonObject, "id");
	}
	public abstract int getVersion();

	@Nullable
	public abstract JsonElement getCustom(String key);

	@Override
	public JsonElement getInjectedInterfaces() {
		return getCustom(Constants.CustomModJsonKeys.INJECTED_INTERFACE);
	}

	@Override
	public @Nullable String getProvidedJavadocPath() {
		JsonElement ret = getCustom(Constants.CustomModJsonKeys.PROVIDED_JAVADOC);
		return ret != null ? ret.getAsString() : null;
	}

	@Override
	public @Nullable String getModName() {
		return ModMetadataUtils.readStringOrNull(jsonObject, "name");
	}

	@Override
	public final ModJsonSource getSource() {
		return source;
	}

	@Override
	public final String toString() {
		return getClass().getName() + "[id=%s, version=%s]".formatted(getId(), getVersion());
	}

	@Override
	public JsonObject stripNestedJars(JsonObject json) {
		json.remove("jars");
		return json;
	}

	@Override
	public JsonObject addNestedJars(JsonObject json, List<String> files) {
		JsonArray nested = json.has("jars") ? json.getAsJsonArray("jars") : new JsonArray();

		for (String nestedJarPath: files) {
			for (JsonElement nestedJar : nested) {
				JsonObject jar = nestedJar.getAsJsonObject();

				if (jar.has("file") && jar.get("file").getAsString().equals(nestedJarPath)) {
					throw new IllegalStateException("Cannot nest 2 jars at the same path " + nestedJarPath);
				}
			}

			JsonObject entry = new JsonObject();
			entry.addProperty("file", nestedJarPath);
			nested.add(entry);
		}
		json.add("jars", nested);
		return json;
	}

	@Override
	public final int hashCode() {
		return Objects.hash(getId(), getVersion());
	}

	@VisibleForTesting
	public abstract non-sealed class Mockable extends FabricModJson {
		private Mockable() {
			super(null, null);
			throw new AssertionError();
		}
	}
}
