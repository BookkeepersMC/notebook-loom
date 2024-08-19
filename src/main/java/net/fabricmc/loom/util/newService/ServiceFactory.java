package net.fabricmc.loom.util.newService;

import org.gradle.api.provider.Property;

public interface ServiceFactory {

	default <O extends Service.Options, S extends Service<O>> S get(Property<O> options) {
		return get(options.get());
	}

	<O extends Service.Options, S extends Service<O>> S get(O options);
}
