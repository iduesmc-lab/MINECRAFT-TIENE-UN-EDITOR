package com.iduesmc.director.scene;

import com.iduesmc.director.Cinema;
import com.iduesmc.director.PlayerTake;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** «CAMBIO DE GÉNERO». For a while, the survival game is a horror film, a western, a musical... */
public class GenreScene extends Scene {
	public static final String NAMED_TAG = "director_genre_named";

	private final Genre genre;
	private final boolean orderedByPlayer;
	private final List<UUID> renamed = new ArrayList<>();

	public GenreScene(Genre genre, boolean orderedByPlayer) {
		super(20 * 45);
		this.genre = genre;
		this.orderedByPlayer = orderedByPlayer;
	}

	public Genre genre() {
		return genre;
	}

	public boolean orderedByPlayer() {
		return orderedByPlayer;
	}

	@Override
	public boolean isGenre() {
		return true;
	}

	@Override
	public String name() {
		return "Género: " + genre.title;
	}

	public static Genre randomGenre(Random random, Genre except) {
		Genre[] all = Genre.values();
		Genre picked;
		do {
			picked = all[random.nextInt(all.length)];
		} while (picked == except);
		return picked;
	}

	@Override
	public void start(ServerPlayerEntity player, PlayerTake take) {
		Cinema.card(player, "CAMBIO DE GÉNERO: " + genre.title, genre.tagline, genre.color, 3500);
		if (genre == Genre.ANIME) {
			Cinema.say(player, "¡NANI?! El protagonista ha desbloqueado su forma final.");
		} else if (genre == Genre.TERROR) {
			Cinema.say(player, "Bajad las luces. Más. MÁS.");
		}
	}

	@Override
	protected void onTick(ServerPlayerEntity player, PlayerTake take) {
		if (age % 20 != 0) {
			return;
		}
		ServerWorld world = player.getServerWorld();
		Random random = player.getRandom();
		List<MobEntity> cast = world.getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(16),
				mob -> genre == Genre.MUSICAL || genre == Genre.DOCUMENTAL || mob instanceof HostileEntity);
		for (MobEntity mob : cast) {
			if (!mob.hasCustomName()) {
				mob.setCustomName(Text.literal(genre.mobName));
				mob.addCommandTag(NAMED_TAG);
				renamed.add(mob.getUuid());
			}
		}

		switch (genre) {
			case TERROR -> {
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 60, 0, false, false));
				if (random.nextInt(4) == 0) {
					Cinema.sound(player, SoundEvents.AMBIENT_CAVE, 1.0f, 0.8f + random.nextFloat() * 0.4f);
				}
				if (age % 60 == 0) {
					Cinema.sound(player, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 1.0f, 1.0f);
				}
			}
			case WESTERN -> {
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, 0, false, false));
				note(player, SoundEvents.BLOCK_NOTE_BLOCK_BANJO, random);
				particles(world, player, ParticleTypes.WHITE_ASH, 30, 6);
			}
			case ROMANCE -> {
				for (MobEntity mob : cast) {
					mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1, false, false));
					mob.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 40, 1, false, false));
					world.spawnParticles(ParticleTypes.HEART, mob.getX(), mob.getEyeY() + 0.5, mob.getZ(), 2, 0.3, 0.3, 0.3, 0);
				}
				particles(world, player, ParticleTypes.HEART, 3, 1.5);
				note(player, SoundEvents.BLOCK_NOTE_BLOCK_CHIME, random);
			}
			case MUSICAL -> {
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 40, 1, false, false));
				for (MobEntity mob : cast) {
					if (mob.isOnGround()) {
						mob.addVelocity(0, 0.45, 0);
						mob.velocityModified = true;
					}
					world.spawnParticles(ParticleTypes.NOTE, mob.getX(), mob.getEyeY() + 0.6, mob.getZ(), 1, 0, 0, 0, random.nextDouble());
				}
				note(player, SoundEvents.BLOCK_NOTE_BLOCK_HARP, random);
				note(player, SoundEvents.BLOCK_NOTE_BLOCK_BASEDRUM, random);
			}
			case ANIME -> {
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, 1, false, false));
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 40, 0, false, false));
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 40, 1, false, false));
				particles(world, player, ParticleTypes.ELECTRIC_SPARK, 25, 1.0);
				particles(world, player, ParticleTypes.CRIT, 15, 1.0);
			}
			case DOCUMENTAL -> {
				if (age % 120 == 0) {
					narrate(player, random);
				}
			}
		}
	}

	private void narrate(ServerPlayerEntity player, Random random) {
		String item = player.getMainHandStack().isEmpty() ? "sus propias manos" : player.getMainHandStack().getName().getString();
		List<String> lines = List.of(
				"Aquí observamos al jugador en su hábitat natural: un agujero que él mismo ha cavado.",
				"Fíjense cómo sujeta " + item + " con la torpeza propia de su especie.",
				"Silencio... si hacemos ruido, podría ponerse a picar piedra otra vez.",
				"El jugador acumula bloques compulsivamente. Los científicos aún no saben por qué.",
				"La naturaleza es cruel. Y el jugador, con " + Math.round(player.getHealth()) + " de vida, también.");
		player.sendMessage(Text.literal("[🎙 Narrador] ").formatted(Formatting.DARK_GREEN, Formatting.BOLD)
				.append(Text.literal(lines.get(random.nextInt(lines.size()))).formatted(Formatting.GREEN, Formatting.ITALIC)));
	}

	private static void note(ServerPlayerEntity player, RegistryEntry<SoundEvent> sound, Random random) {
		Cinema.sound(player, sound, 0.7f, (float) Math.pow(2.0, (random.nextInt(12) - 6) / 12.0));
	}

	private static void particles(ServerWorld world, ServerPlayerEntity player, ParticleEffect effect, int count, double spread) {
		world.spawnParticles(effect, player.getX(), player.getY() + 1, player.getZ(), count, spread, spread, spread, 0.02);
	}

	@Override
	public void end(ServerPlayerEntity player, PlayerTake take) {
		super.end(player, take);
		ServerWorld world = player.getServerWorld();
		for (UUID id : renamed) {
			if (world.getEntity(id) instanceof MobEntity mob && mob.getCommandTags().contains(NAMED_TAG)) {
				mob.setCustomName(null);
				mob.removeCommandTag(NAMED_TAG);
			}
		}
		if (!player.isRemoved()) {
			Cinema.actionbar(player, Text.literal("🎬 Fin de «" + genre.title.toLowerCase() + "». Volvemos al survival.").formatted(Formatting.GRAY));
		}
	}
}
