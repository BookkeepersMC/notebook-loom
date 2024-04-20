/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024 FabricMC
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

package com.bookkeepersmc.loom.util.nmj;

import com.bookkeepersmc.loom.configuration.metadata.ModEnvironment;
import com.bookkeepersmc.loom.util.metadata.ModJsonSource;
import com.bookkeepersmc.loom.util.metadata.ModMetadataUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.google.gson.JsonPrimitive;


import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class NotebookModJsonV1 extends NotebookModJson {
	NotebookModJsonV1(JsonObject jsonObject, ModJsonSource source) {
		super(jsonObject, source);
	}

	@Override
	public String getId() {
		return ModMetadataUtils.readString(loader, "id");
	}

	@Override
	public String getModVersion() {
		return ModMetadataUtils.readString(loader, "version");
	}

	@Override
	public @Nullable String getModName() {
		if (loader.has("metadata")) {
			JsonObject metadata = loader.getAsJsonObject("metadata");

			if (metadata.has("name")) {
				return ModMetadataUtils.readString(metadata, "name");
			}
		}
		return null;
	}

	public @Nullable JsonElement getCustom(String key) {
		return jsonObject.get(key);
	}

	@Override
	public List<String> getMixinConfigurations() {
		final JsonElement mixins = jsonObject.get("mixin");

		if (mixins == null) {
			return Collections.emptyList();
		} else if (mixins.isJsonArray()) {
			return StreamSupport.stream(mixins.getAsJsonArray().spliterator(), false)
					.map(NotebookModJsonV1::readMixinElement)
					.collect(Collectors.toList());
		} else {
			throw new RuntimeException("Incorrect Notebook Mod Json format; expected 'mixin' to be a string or array");
		}
	}

	private static String readMixinElement(JsonElement jsonElement) {
		if (jsonElement instanceof JsonPrimitive str) {
			return str.getAsString();
		} else if (jsonElement instanceof JsonObject obj) {
			return obj.get("config").getAsString();
		} else {
			throw new RuntimeException("Expected mixin element to be an object or string");
		}
	}

	@Override
	public Map<String, ModEnvironment> getClassTweakers() {
		final JsonElement jsonElement = jsonObject.get("access_widener");

		if (jsonElement != null) {
			if (jsonObject.isJsonArray()) {
				JsonArray jsonArray = jsonElement.getAsJsonArray();

				if (jsonArray.size() > 1) {
					throw new UnsupportedOperationException("Notebook loom does not support more than one access widener per mod.");
				} else if (jsonArray.size() == 1) {
					return Map.of(jsonArray.get(0).getAsString(), ModEnvironment.UNIVERSAL);
				}
			} else if (jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isString()) {
				return Map.of(jsonElement.getAsString(), ModEnvironment.UNIVERSAL);
			}
		}
		return Collections.emptyMap();
	}

	@Override
	public @Nullable JsonElement getInjectedInterfaces() {
		final JsonElement element = getCustom("notebook_loom");

		if (element != null) {
			return element.getAsJsonObject().get("injected_interfaces");
		} else {
			return null;
		}
	}

	@Override
	public String getProvidedJavadocPath() {
		final JsonElement element = getCustom("notebook_loom");

		if (element != null) {
			return ModMetadataUtils.readStringOrNull(element.getAsJsonObject(), "provided_javadoc");
		} else {
			return null;
		}
	}

	@Override
	public JsonObject stripNestedJars(JsonObject json) {
		JsonObject loader = json.has("notebook_loader") ? json.get("notebook_loader").getAsJsonObject() : new JsonObject();
		loader.remove("jars");
		return json;
	}

	@Override
	public JsonObject addNestedJars(JsonObject json, List<String> files) {
		JsonObject loader = json.has("notebook_loader") ? json.get("notebook_loader").getAsJsonObject() : new JsonObject();
		JsonArray nested = loader.has("jars") ? json.get("jars").getAsJsonArray() : new JsonArray();

		for (String nestedPath : files) {
			for (JsonElement nestedJar : nested) {
				if (nestedPath.equals(nestedJar.getAsString())) {
					throw new IllegalStateException("Cannot nest 2 jars at the same path " + nestedPath);
				}
			}
			nested.add(nestedPath);
		}
		loader.add("jars", nested);
		return json;
	}
}
