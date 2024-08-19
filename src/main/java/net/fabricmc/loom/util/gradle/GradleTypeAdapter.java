package net.fabricmc.loom.util.gradle;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

public class GradleTypeAdapter implements TypeAdapterFactory {
	public static final Gson GSON = new Gson().newBuilder()
			.registerTypeAdapterFactory(new GradleTypeAdapter())
			.create();

	@Override
	public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
		final Class<? super T> rawClass = type.getRawType();

		if (FileCollection.class.isAssignableFrom(rawClass)) {
			return new FileCollectionTypeAdapter();
		} else if (RegularFileProperty.class.isAssignableFrom(rawClass)) {
			return new RegularFilePropertyTypeAdapter();
		} else if (ListProperty.class.isAssignableFrom(rawClass)) {
			return new ListPropertyTypeAdapter(gson);
		} else if (MapProperty.class.isAssignableFrom(rawClass)) {
			return new MapPropertyTypeAdapter(gson);
		} else if (Property.class.isAssignableFrom(rawClass)) {
			return new PropertyTypeAdapter(gson);
		}

		return null;
	}

	private static final class PropertyTypeAdapter<T extends Property<?>> extends WriteOnlyTypeAdapter<T> {
		private final Gson gson;

		private PropertyTypeAdapter(Gson gson) {
			this.gson = gson;
		}

		@Override
		public void write(JsonWriter out, T property) throws IOException {
			final Object o = property.get();
			final TypeAdapter adapter = gson.getAdapter(o.getClass());
			adapter.write(out, o);
		}
	}

	private static final class FileCollectionTypeAdapter<T extends FileCollection> extends WriteOnlyTypeAdapter<T> {
		@Override
		public void write(JsonWriter out, T fileCollection) throws IOException {
			out.beginArray();

			final List<String> files = fileCollection.getFiles().stream()
					.map(File::getAbsolutePath)
					.sorted()
					.toList();

			for (String file : files) {
				out.value(file);
			}

			out.endArray();
		}
	}

	private static final class RegularFilePropertyTypeAdapter<T extends RegularFileProperty> extends WriteOnlyTypeAdapter<T> {
		@Override
		public void write(JsonWriter out, T property) throws IOException {
			final File file = property.get().getAsFile();
			out.value(file.getAbsolutePath());
		}
	}

	private static final class ListPropertyTypeAdapter<T extends ListProperty<?>> extends WriteOnlyTypeAdapter<T> {
		private final Gson gson;

		private ListPropertyTypeAdapter(Gson gson) {
			this.gson = gson;
		}

		@Override
		public void write(JsonWriter out, T property) throws IOException {
			List<?> objects = property.get();
			out.beginArray();

			for (Object o : objects) {
				final TypeAdapter adapter = gson.getAdapter(o.getClass());
				adapter.write(out, o);
			}

			out.endArray();
		}
	}

	private static final class MapPropertyTypeAdapter<T extends MapProperty<?, ?>> extends WriteOnlyTypeAdapter<T> {
		private final Gson gson;

		private MapPropertyTypeAdapter(Gson gson) {
			this.gson = gson;
		}

		@Override
		public void write(JsonWriter out, T property) throws IOException {
			out.beginObject();

			for (Map.Entry<?, ?> entry : property.get().entrySet()) {
				Object key = entry.getKey();
				Object value = entry.getValue();

				if (!(key instanceof String)) {
					throw new UnsupportedOperationException("Map keys must be strings");
				}

				out.name(entry.getKey().toString());
				final TypeAdapter adapter = gson.getAdapter(value.getClass());
				adapter.write(out, value);
			}

			out.endObject();
		}
	}

	private abstract static class WriteOnlyTypeAdapter<T> extends TypeAdapter<T> {
		@Override
		public final T read(JsonReader in) {
			throw new UnsupportedOperationException("This type adapter is write-only");
		}
	}
}
