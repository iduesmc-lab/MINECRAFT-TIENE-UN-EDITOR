package com.iduesmc.director;

import com.iduesmc.director.net.CinemaCardPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;

import java.util.List;

/** Everything the audience sees or hears from the director: carteles, chat lines and sounds. */
public final class Cinema {
	public static final int RED = 0xFF5555;
	public static final int GOLD = 0xFFAA00;
	public static final int CARDBOARD = 0xC8A165;
	public static final int CYAN = 0x55FFFF;
	public static final int PINK = 0xFF77CC;
	public static final int WHITE = 0xFFFFFF;

	private Cinema() {
	}

	/** Shows a big director card. Modded clients get letterbox bars and a clapperboard; vanilla ones a title. */
	public static void card(ServerPlayerEntity player, String title, String subtitle, int color, int durationMs) {
		int take = Director.INSTANCE.take(player).takeNumber;
		if (ServerPlayNetworking.canSend(player, CinemaCardPayload.ID)) {
			ServerPlayNetworking.send(player, new CinemaCardPayload(title, subtitle, color, durationMs, take));
		} else {
			int stay = Math.max(20, durationMs / 50 - 20);
			player.networkHandler.sendPacket(new TitleFadeS2CPacket(5, stay, 15));
			player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(subtitle).formatted(Formatting.WHITE)));
			player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(title)
					.styled(s -> s.withColor(TextColor.fromRgb(color)).withBold(true))));
		}
		clap(player);
	}

	/** The director shouts something in chat. */
	public static void say(ServerPlayerEntity player, String line) {
		player.sendMessage(Text.literal("[🎬 Director] ").formatted(Formatting.GOLD, Formatting.BOLD)
				.append(Text.literal(line).formatted(Formatting.YELLOW)));
	}

	public static void say(ServerPlayerEntity player, List<String> lines, Random random) {
		say(player, lines.get(random.nextInt(lines.size())));
	}

	public static void actionbar(ServerPlayerEntity player, Text text) {
		player.sendMessage(text, true);
	}

	public static void sound(ServerPlayerEntity player, SoundEvent sound, float volume, float pitch) {
		player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundCategory.MASTER, volume, pitch);
	}

	/** Some vanilla sounds (note blocks, goat horns, cave ambience) are registry entries rather than plain events. */
	public static void sound(ServerPlayerEntity player, RegistryEntry<SoundEvent> sound, float volume, float pitch) {
		sound(player, sound.value(), volume, pitch);
	}

	/** El sonido de la claqueta. */
	public static void clap(ServerPlayerEntity player) {
		sound(player, SoundEvents.BLOCK_WOODEN_TRAPDOOR_CLOSE, 1.0f, 1.6f);
	}
}
