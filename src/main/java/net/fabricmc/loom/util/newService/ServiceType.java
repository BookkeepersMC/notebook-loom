package net.fabricmc.loom.util.newService;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

public record ServiceType<O extends Service.Options, S extends Service<O>>(Class<O> optionsClass, Class<S> serviceClass) {

	public Provider<O> create(Project project, Action<O> action) {
		return project.provider(() -> {
			O options = project.getObjects().newInstance(optionsClass);
			options.getServiceClass().set(serviceClass.getName());
			options.getServiceClass().finalizeValue();
			action.execute(options);
			return options;
		});
	}
}
