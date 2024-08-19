package com.bookkeepersmc.loom.test.unit.service

import com.bookkeepersmc.loom.test.util.GradleTestUtil
import net.fabricmc.loom.util.newService.ScopedServiceFactory
import net.fabricmc.loom.util.newService.Service
import net.fabricmc.loom.util.newService.ServiceType
import org.gradle.api.provider.Property
import spock.lang.Specification

abstract class ServiceTestBase extends Specification {
    ScopedServiceFactory factory

    def setup() {
        factory = new ScopedServiceFactory()
    }

    def cleanup() {
        factory.close()
        factory = null
    }

    static Property<String> serviceClassProperty(ServiceType<? extends Service.Options, ? extends Service> type) {
        return GradleTestUtil.mockProperty(type.serviceClass().name)
    }
}
