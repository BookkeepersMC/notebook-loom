package net.fabricmc.loom.util.newService;

import java.io.Closeable;
import java.io.IOException;

import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;

public final class ExampleService extends Service<ExampleService.Options> implements Closeable {
	public static ServiceType<Options, ExampleService> TYPE = new ServiceType<>(Options.class, ExampleService.class);

	// Options use Gradle's Property's thus can be used in task inputs.
	public interface Options extends Service.Options {
		@Nested
		Property<AnotherService.Options> getNested();
	}

	// Options can be created using data from the Project
	static Provider<Options> createOptions(Project project) {
		return TYPE.create(project, o -> {
			o.getNested().set(AnotherService.createOptions(project, "example"));
		});
	}

	// An example of how a service could be used, this could be within a task action.
	// ServiceFactory would be similar to the existing ScopedSharedServiceManager
	// Thus if a service with the same options has previously been created it will be reused.
	static void howToUse(Options options, ServiceFactory factory) {
		ExampleService exampleService = factory.get(options);
		exampleService.doSomething();
	}

	public ExampleService(Options options, ServiceFactory serviceFactory) {
		super(options, serviceFactory);
	}

	public void doSomething() {
		// The service factory used to the creation the current service can be used to get or create other services based on the current service's options.
		AnotherService another = getServiceFactory().get(getOptions().getNested());
		System.out.println("ExampleService: " + another.getExample());
	}

	@Override
	public void close() throws IOException {
		// Anything that needs to be cleaned up when the service is no longer needed.
	}

	public static final class AnotherService extends Service<AnotherService.Options> {
		public static ServiceType<Options, AnotherService> TYPE = new ServiceType<>(Options.class, AnotherService.class);

		public interface Options extends Service.Options {
			@Input
			Property<String> getExample();
		}

		static Provider<AnotherService.Options> createOptions(Project project, String example) {
			return TYPE.create(project, o -> {
				o.getExample().set(example);
			});
		}

		public AnotherService(Options options, ServiceFactory serviceFactory) {
			super(options, serviceFactory);
		}

		// Services can expose any methods they wish, either to return data or do a job.
		public String getExample() {
			return getOptions().getExample().get();
		}
	}
}
