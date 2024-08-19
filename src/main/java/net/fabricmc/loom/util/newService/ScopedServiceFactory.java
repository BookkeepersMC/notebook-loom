package net.fabricmc.loom.util.newService;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import net.fabricmc.loom.util.gradle.GradleTypeAdapter;

public final class ScopedServiceFactory implements ServiceFactory, Closeable {
	private final Map<Service.Options, Service<?>> servicesIdentityMap = new IdentityHashMap<>();
	private final Map<String, Service<?>> servicesJsonMap = new HashMap<>();

	@Override
	public <O extends Service.Options, S extends Service<O>> S get(O options) {
		// First check if the service is already created, using the identity map saving the need to serialize the options
		//noinspection unchecked
		S service = (S) servicesIdentityMap.get(options);

		if (service != null) {
			return service;
		}

		// TODO skip serialization if we know there is no service with the same type

		// If the service is not already created, serialize the options and check the json map as it may be an equivalent service
		String key = getOptionsCacheKey(options);
		//noinspection unchecked
		service = (S) servicesJsonMap.get(key);

		if (service != null) {
			return service;
		}

		service = createService(options, this);

		servicesIdentityMap.put(options, service);
		servicesJsonMap.put(key, service);

		return service;
	}

	private static <O extends Service.Options, S extends Service<O>> S createService(O options, ServiceFactory serviceFactory) {
		// We need to create the service from the provided options
		final Class<? extends S> serviceClass;

		// Find the service class
		try {
			//noinspection unchecked
			serviceClass = (Class<? extends S>) Class.forName(options.getServiceClass().get());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Failed to find service class: " + options.getServiceClass().get(), e);
		}

		try {
			// Check there is only 1 constructor
			if (serviceClass.getDeclaredConstructors().length != 1) {
				throw new RuntimeException("Service class must have exactly 1 constructor");
			}

			// Check the constructor takes the correct types, the options class and a ScopedServiceFactory
			Class<?>[] parameterTypes = serviceClass.getDeclaredConstructors()[0].getParameterTypes();

			if (parameterTypes.length != 2 || !parameterTypes[0].isAssignableFrom(options.getClass()) || !parameterTypes[1].isAssignableFrom(ServiceFactory.class)) {
				throw new RuntimeException("Service class" + serviceClass.getName() + " constructor must take the options class and a ScopedServiceFactory");
			}

			//noinspection unchecked
			return (S) serviceClass.getDeclaredConstructors()[0].newInstance(options, serviceFactory);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException("Failed to create service instance", e);
		}
	}

	private String getOptionsCacheKey(Service.Options options) {
		return GradleTypeAdapter.GSON.toJson(options);
	}

	@Override
	public void close() throws IOException {
		for (Service<?> service : servicesIdentityMap.values()) {
			if (service instanceof Closeable closeable) {
				closeable.close();
			}
		}

		servicesIdentityMap.clear();
		servicesJsonMap.clear();
	}
}