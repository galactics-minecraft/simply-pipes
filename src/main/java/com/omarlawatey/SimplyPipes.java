package com.omarlawatey;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.omarlawatey.registries.ItemsInitializer;

public class SimplyPipes implements ModInitializer {
	public static final String MOD_ID = "simply-pipes";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing SimplyPipes");

		ItemsInitializer.initializeAll();
	}
}