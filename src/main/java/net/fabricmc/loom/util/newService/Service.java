package net.fabricmc.loom.util.newService;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.jetbrains.annotations.ApiStatus;

public abstract class Service<O extends Service.Options> {
	private final O options;
	private final ServiceFactory serviceFactory;

	public Service(O options, ServiceFactory serviceFactory) {
		this.options = options;
		this.serviceFactory = serviceFactory;
	}

	protected final O getOptions() {
		return options;
	}

	protected ServiceFactory getServiceFactory() {
		return serviceFactory;
	}

	/**
	 * The base type of options class for a service.
	 */
	public interface Options {
		@Input
		@ApiStatus.Internal
		Property<String> getServiceClass();
	}
}
