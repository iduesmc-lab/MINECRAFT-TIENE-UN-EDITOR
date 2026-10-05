package com.iduesmc.director.client;

import com.iduesmc.director.net.CinemaCardPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class DirectorClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(CinemaCardPayload.ID, (payload, context) -> CinemaHud.show(payload));
		HudRenderCallback.EVENT.register(CinemaHud::render);
	}
}
