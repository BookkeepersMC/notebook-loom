package com.bookkeepersmc.loom.test.unit.service

import groovy.transform.InheritConstructors
import groovy.transform.TupleConstructor
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import spock.lang.Specification

import com.bookkeepersmc.loom.test.util.GradleTestUtil
import net.fabricmc.loom.util.newService.ScopedServiceFactory
import net.fabricmc.loom.util.newService.Service
import net.fabricmc.loom.util.newService.ServiceType

class ScopedServiceFactoryTest extends Specification {
    def "create service"() {
        given:
        def options = new TestServiceOptions(GradleTestUtil.mockProperty("hello"))
        def factory = new ScopedServiceFactory()

        when:
        TestService service = factory.get(options)

        then:
        service.getExample() == "hello"

        cleanup:
        factory.close()
    }

    def "reuse service"() {
        given:
        def options = new TestServiceOptions(GradleTestUtil.mockProperty("hello"))
        def factory = new ScopedServiceFactory()

        when:
        TestService service = factory.get(options)
        TestService service2 = factory.get(options)

        then:
        service === service2

        cleanup:
        factory.close()
    }

    def "reuse service different options instance"() {
        given:
        def options = new TestServiceOptions(GradleTestUtil.mockProperty("hello"))
        def options2 = new TestServiceOptions(GradleTestUtil.mockProperty("hello"))
        def factory = new ScopedServiceFactory()

        when:
        TestService service = factory.get(options)
        TestService service2 = factory.get(options2)

        then:
        service === service2

        cleanup:
        factory.close()
    }

    def "Separate instances"() {
        given:
        def options = new TestServiceOptions(GradleTestUtil.mockProperty("hello"))
        def options2 = new TestServiceOptions(GradleTestUtil.mockProperty("world"))
        def factory = new ScopedServiceFactory()

        when:
        TestService service = factory.get(options)
        TestService service2 = factory.get(options2)

        then:
        service !== service2
        service.example == "hello"
        service2.example == "world"

        cleanup:
        factory.close()
    }

    def "close service"() {
        given:
        def options = new TestServiceOptions(GradleTestUtil.mockProperty("hello"))
        def factory = new ScopedServiceFactory()

        when:
        TestService service = factory.get(options)
        factory.close()

        then:
        service.closed
    }

    @InheritConstructors
    static class TestService extends Service<Options> implements Closeable {
        static ServiceType<TestService.Options, TestService> TYPE = new ServiceType(TestService.Options.class, TestService.class)

        interface Options extends Service.Options {
            @Input
            Property<String> getExample();
        }

        boolean closed = false

        String getExample() {
            return options.example.get()
        }

        @Override
        void close() throws Exception {
            closed = true
        }
    }

    @TupleConstructor
    static class TestServiceOptions implements TestService.Options {
        Property<String> example
        Property<String> serviceClass = ServiceTestBase.serviceClassProperty(TestService.TYPE)
    }
}
