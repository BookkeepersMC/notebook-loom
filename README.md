# Notebook Loom

A [Gradle](https://gradle.org/) plugin to setup a deobfuscated development environment for Minecraft mods.

* VERY EARLY BETA!!!!!

## Use Loom to develop mods
1. Add the Bookkeepers repo in your settings.gradle file
```gradle
maven {url = "https://bookkeepersmc.github.io" }
```
2. Add the plugin
```gradle
id 'notebook-loom' version '1.0-SNAPSHOT'
```