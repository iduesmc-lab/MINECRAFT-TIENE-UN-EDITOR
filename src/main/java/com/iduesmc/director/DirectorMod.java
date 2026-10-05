package com.iduesmc.director;

import com.iduesmc.director.item.ModItems;
import com.iduesmc.director.net.CinemaCardPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectorMod implements ModInitializer {
	public static final String MOD_ID = "director";
	public static final Logger LOGGER = LoggerFactory.getLogger("Director");

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModItems.register();
		PayloadTypeRegistry.playS2C().register(CinemaCardPayload.ID, CinemaCardPayload.CODEC);
		Director.INSTANCE.registerEvents();
		DirectorCommand.register();
		LOGGER.info("¡Silencio en el set! El director ha llegado.");
	}
}
