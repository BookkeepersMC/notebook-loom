package net.fabricmc.loom.task.service;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.configuration.providers.mappings.MappingConfiguration;

import net.fabricmc.loom.util.TinyRemapperHelper;
import net.fabricmc.loom.util.newService.Service;
import net.fabricmc.loom.util.newService.ServiceFactory;
import net.fabricmc.loom.util.newService.ServiceType;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.tinyremapper.IMappingProvider;

import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

public final class NewMappingsService extends Service<NewMappingsService.Options> implements Closeable {
	public static ServiceType<Options, NewMappingsService> TYPE = new ServiceType<>(Options.class, NewMappingsService.class);

	public interface Options extends Service.Options {
		@InputFile
		RegularFileProperty getMappingsFile();
		@Input
		Property<String> getFrom();
		@Input
		Property<String> getTo();
		@Input
		Property<Boolean> getRemapLocals();
	}

	public static Provider<Options> createOptions(Project project, Path mappingsFile, String from, String to, boolean remapLocals) {
		return TYPE.create(project, o -> {
			o.getMappingsFile().set(project.file(mappingsFile));
			o.getFrom().set(from);
			o.getTo().set(to);
			o.getRemapLocals().set(remapLocals);
		});
	}

	/**
	 * Returns options for creating a new mappings service, using the mappings as specified in the project's mapping configuration.
	 */
	public static Provider<Options> createOptionsWithProjectMappings(Project project, String from, String to) {
		final MappingConfiguration mappingConfiguration = LoomGradleExtension.get(project).getMappingConfiguration();
		return createOptions(project, mappingConfiguration.tinyMappings, from, to, false);
	}

	public NewMappingsService(Options options, ServiceFactory serviceFactory) {
		super(options, serviceFactory);
	}

	private IMappingProvider mappingProvider = null;
	private MemoryMappingTree memoryMappingTree = null;

	public IMappingProvider getMappingsProvider() {
		if (mappingProvider == null) {
			try {
				mappingProvider = TinyRemapperHelper.create(
						getMappingsPath(),
						getFrom(),
						getTo(),
						getOptions().getRemapLocals().get()
				);
			} catch (IOException e) {
				throw new UncheckedIOException("Failed to read mappings from: " + getMappingsPath(), e);
			}
		}

		return mappingProvider;
	}

	public MemoryMappingTree getMemoryMappingTree() {
		if (memoryMappingTree == null) {
			memoryMappingTree = new MemoryMappingTree();

			try {
				MappingReader.read(getMappingsPath(), memoryMappingTree);
			} catch (IOException e) {
				throw new UncheckedIOException("Failed to read mappings from: " + getMappingsPath(), e);
			}
		}

		return memoryMappingTree;
	}

	public String getFrom() {
		return getOptions().getFrom().get();
	}

	public String getTo() {
		return getOptions().getTo().get();
	}

	public Path getMappingsPath() {
		return getOptions().getMappingsFile().get().getAsFile().toPath();
	}

	@Override
	public void close() {
		mappingProvider = null;
	}
}
