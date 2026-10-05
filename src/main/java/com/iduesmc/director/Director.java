package com.iduesmc.director;

import com.iduesmc.director.scene.BudgetCutScene;
import com.iduesmc.director.scene.GenreScene;
import com.iduesmc.director.scene.PlotTwistScene;
import com.iduesmc.director.scene.Scene;
import com.iduesmc.director.scene.SponsorScene;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.random.Random;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The invisible director. Watches every player, decides when the audience is bored and cuts to a new scene.
 */
public final class Director {
	public static final Director INSTANCE = new Director();

	/** Seconds of nothing happening before the bar empties completely is roughly 100 / (BASE + IDLE). */
	private static final float BOREDOM_PER_SECOND = 0.7f;
	private static final float BOREDOM_IDLE_PER_SECOND = 1.0f;
	private static final float BOREDOM_PER_STONE = 2.0f;
	private static final int SCENE_COOLDOWN = 20 * 90;
	private static final int SPONSOR_COOLDOWN = 20 * 60 * 3;

	private final Map<UUID, PlayerTake> takes = new HashMap<>();
	private final MegaphoneController megaphone = new MegaphoneController();
	private final Random random = Random.create();
	private long serverTicks;

	private Director() {
	}

	public PlayerTake take(ServerPlayerEntity player) {
		return takes.computeIfAbsent(player.getUuid(), PlayerTake::new);
	}

	public MegaphoneController megaphone() {
		return megaphone;
	}

	public void registerEvents() {
		ServerTickEvents.END_SERVER_TICK.register(this::tick);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				cut(player, take(player));
			}
			megaphone.restoreTickRate(server);
			takes.clear();
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			PlayerTake take = take(player);
			take.bar.addPlayer(player);
			take.joinCardDelay = 40;
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			PlayerTake take = take(player);
			cut(player, take);
			take.bar.removePlayer(player);
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			PlayerTake take = take(newPlayer);
			take.bar.removePlayer(oldPlayer);
			take.bar.addPlayer(newPlayer);
			if (!alive) {
				take.joinCardDelay = 10;
			}
		});

		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (player instanceof ServerPlayerEntity serverPlayer) {
				onBlockBroken(serverPlayer, take(serverPlayer), state);
			}
		});

		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayerEntity player) {
				return StuntDouble.allowDeath(player, take(player), source);
			}
			return true;
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayerEntity player) {
				PlayerTake take = take(player);
				cut(player, take);
				take.takeNumber++;
				take.boredom = 0;
			}
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (entity instanceof ServerPlayerEntity player && damageTaken > 0) {
				PlayerTake take = take(player);
				take.addBoredom(-damageTaken * 2.5f);
				Entity attacker = source.getAttacker();
				if (attacker != null && attacker.getCommandTags().contains(MegaphoneController.SECURITY_TAG)) {
					megaphone.onHitBySecurity(player, take, attacker);
				}
			}
		});
		ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killed) -> {
			if (entity instanceof ServerPlayerEntity player && killed instanceof HostileEntity) {
				take(player).addBoredom(-10);
			}
		});

		ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
			PlayerTake take = take(sender);
			if (take.scene instanceof SponsorScene sponsor) {
				sponsor.onChat(sender, message.getContent().getString());
			}
		});
	}

	// ---------------------------------------------------------------- ticking

	private void tick(MinecraftServer server) {
		serverTicks++;
		megaphone.tickGlobal(server);
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			tickPlayer(player, take(player));
		}
		if (serverTicks % 20 == 0) {
			Props.sweepOrphans(server);
		}
	}

	private void tickPlayer(ServerPlayerEntity player, PlayerTake take) {
		if (take.joinCardDelay > 0 && --take.joinCardDelay == 0) {
			Cinema.card(player, "TOMA " + take.takeNumber, take.takeNumber == 1 ? "¡Y... ACCIÓN!" : "Desde el principio. Y esta vez, con sentimiento.", Cinema.WHITE, 2500);
		}
		if (take.scene != null && !take.scene.tick(player, take)) {
			Scene finished = take.scene;
			take.scene = null;
			finished.end(player, take);
			take.directorCooldown = SCENE_COOLDOWN;
		}
		if (take.genre != null && !take.genre.tick(player, take)) {
			Scene finished = take.genre;
			take.genre = null;
			finished.end(player, take);
		}
		if (take.directorCooldown > 0) {
			take.directorCooldown--;
		}
		if (take.stuntCooldown > 0) {
			take.stuntCooldown--;
		}
		if (take.sponsorCooldown > 0) {
			take.sponsorCooldown--;
		}
		megaphone.tickPlayer(player, take, serverTicks);
		if (serverTicks % 20 == 0) {
			everySecond(player, take);
		}
	}

	private void everySecond(ServerPlayerEntity player, PlayerTake take) {
		take.history.addLast(new PlayerTake.Snapshot(player.getServerWorld().getRegistryKey(), player.getPos(),
				player.getYaw(), player.getPitch(), player.getHealth()));
		while (take.history.size() > 10) {
			take.history.removeFirst();
		}
		while (!take.stoneMined.isEmpty() && serverTicks - take.stoneMined.peekFirst() > 20 * 60) {
			take.stoneMined.removeFirst();
		}

		boolean actingInSurvival = !player.isCreative() && !player.isSpectator();
		if (actingInSurvival && take.scene == null) {
			take.addBoredom(BOREDOM_PER_SECOND);
			if (take.lastSecondPos != null && take.lastSecondPos.squaredDistanceTo(player.getPos()) < 1.0) {
				take.addBoredom(BOREDOM_IDLE_PER_SECOND);
			}
		}
		take.lastSecondPos = player.getPos();
		take.refreshBar();

		if (actingInSurvival && take.boredom >= PlayerTake.MAX_BOREDOM && take.scene == null && take.directorCooldown == 0) {
			directorCuts(player, take);
		}
	}

	// ---------------------------------------------------------------- scenes

	/** The audience is bored. ¡CORTEN! */
	public void directorCuts(ServerPlayerEntity player, PlayerTake take) {
		float roll = random.nextFloat();
		if (take.stoneMined.size() >= 10 || roll < 0.35f) {
			startScene(player, take, new BudgetCutScene());
		} else if (roll < 0.65f) {
			GenreScene current = take.genre instanceof GenreScene g ? g : null;
			startScene(player, take, new GenreScene(GenreScene.randomGenre(random, current == null ? null : current.genre()), false));
			take.directorCooldown = SCENE_COOLDOWN;
		} else {
			PlotTwistScene.Twist[] twists = PlotTwistScene.Twist.values();
			startScene(player, take, new PlotTwistScene(twists[random.nextInt(twists.length)]));
		}
		take.boredom = 0;
		maybeDropMegaphone(player, take);
	}

	/** Starts a scene, replacing whatever was running in the same slot. */
	public void startScene(ServerPlayerEntity player, PlayerTake take, Scene scene) {
		Scene previous = scene.isGenre() ? take.genre : take.scene;
		if (previous != null) {
			previous.end(player, take);
		}
		if (scene.isGenre()) {
			take.genre = scene;
		} else {
			take.scene = scene;
		}
		scene.start(player, take);
	}

	/** Ends every scene for this player right now. */
	public void cut(ServerPlayerEntity player, PlayerTake take) {
		if (take.scene != null) {
			Scene scene = take.scene;
			take.scene = null;
			scene.end(player, take);
		}
		if (take.genre != null) {
			Scene genre = take.genre;
			take.genre = null;
			genre.end(player, take);
		}
	}

	private void maybeDropMegaphone(ServerPlayerEntity player, PlayerTake take) {
		float chance = take.everStoleMegaphone ? 0.25f : 0.5f;
		if (random.nextFloat() < chance) {
			megaphone.dropNear(player, take);
		}
	}

	private void onBlockBroken(ServerPlayerEntity player, PlayerTake take, BlockState state) {
		if (player.isCreative() || player.isSpectator()) {
			return;
		}
		if (state.isIn(BlockTags.BASE_STONE_OVERWORLD)) {
			take.stoneMined.addLast(serverTicks);
			if (take.scene == null) {
				take.addBoredom(BOREDOM_PER_STONE);
			}
		} else if (state.isIn(BlockTags.DIAMOND_ORES)) {
			if (take.sponsorCooldown == 0 && !(take.scene instanceof SponsorScene)) {
				take.sponsorCooldown = SPONSOR_COOLDOWN;
				startScene(player, take, new SponsorScene());
				maybeDropMegaphone(player, take);
			}
		} else if (Registries.BLOCK.getId(state.getBlock()).getPath().endsWith("_ore")) {
			take.addBoredom(-6);
		}
	}
}
