package com.bookkeepersmc.example;

import com.bookkeepersmc.api.ModInitializer;
import com.mojang.blaze3d.Blaze3D;

public class ExampleMod implements ModInitializer {
	@Override
	public void onInitialize() {
		System.out.println("Hello Notebook World!");
	}
}