package com.bookkeepersmc.loom.bootstrap;

import org.gradle.api.plugins.PluginAware;

public interface BootstrappedPlugin {
	void apply(PluginAware project);
}
