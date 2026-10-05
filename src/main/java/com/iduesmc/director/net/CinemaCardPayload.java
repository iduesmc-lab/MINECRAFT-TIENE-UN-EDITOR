package com.iduesmc.director.net;

import com.iduesmc.director.DirectorMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Tells a modded client to show a "cartel" of the director: letterbox bars, a clapperboard
 * and the scene title. Vanilla clients get plain titles instead (see {@link com.iduesmc.director.Cinema}).
 */
public record CinemaCardPayload(String title, String subtitle, int color, int durationMs, int take) implements CustomPayload {
	public static final CustomPayload.Id<CinemaCardPayload> ID = new CustomPayload.Id<>(DirectorMod.id("cinema_card"));
	public static final PacketCodec<RegistryByteBuf, CinemaCardPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, CinemaCardPayload::title,
			PacketCodecs.STRING, CinemaCardPayload::subtitle,
			PacketCodecs.INTEGER, CinemaCardPayload::color,
			PacketCodecs.VAR_INT, CinemaCardPayload::durationMs,
			PacketCodecs.VAR_INT, CinemaCardPayload::take,
			CinemaCardPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
