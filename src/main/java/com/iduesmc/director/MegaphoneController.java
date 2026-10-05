package com.iduesmc.director;

import com.iduesmc.director.item.ModItems;
import com.iduesmc.director.scene.BudgetCutScene;
import com.iduesmc.director.scene.Genre;
import com.iduesmc.director.scene.GenreScene;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.List;

/**
 * The stolen megaphone: your orders (repetición, cámara lenta, cambio de género) and the director's
 * increasingly desperate attempts to get it back.
 */
public class MegaphoneController {
	public static final String SECURITY_TAG = "director_security";
	private static final float SLOW_MOTION_TICK_RATE = 6f;
	private static final long SLOW_MOTION_MS = 7000;
	private static final int DROPPED_LIFETIME = 20 * 20;

	private static final List<String> RETORTS = List.of(
			"¡¿Quién te ha dado permiso?! ¡El director soy YO!",
			"Eso no estaba en el guion...",
			"Vale, vale. Pero el montaje final es mío.",
			"¡Devuélveme eso! Mi madre me lo regaló.",
			"Te estoy apuntando en la lista de actores difíciles.");

	private boolean slowMotion;
	private long slowMotionUntil;

	// ---------------------------------------------------------------- ticking

	public void tickGlobal(MinecraftServer server) {
		if (slowMotion && System.currentTimeMillis() >= slowMotionUntil) {
			restoreTickRate(server);
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				Cinema.actionbar(player, Text.literal("⏩ Velocidad normal").formatted(Formatting.GRAY));
			}
		}
	}

	public void restoreTickRate(MinecraftServer server) {
		if (slowMotion) {
			server.getTickManager().setTickRate(20f);
			slowMotion = false;
		}
	}

	public void tickPlayer(ServerPlayerEntity player, PlayerTake take, long serverTicks) {
		tickDroppedMegaphone(player, take);
		if (serverTicks % 20 != 0) {
			return;
		}
		boolean has = countMegaphones(player) > 0;
		if (has && !take.hasMegaphone) {
			take.hasMegaphone = true;
			take.droppedMegaphone = null;
			if (!take.everStoleMegaphone) {
				take.everStoleMegaphone = true;
				Cinema.card(player, "¡ROBASTE EL MEGÁFONO!", "Ahora tú das las órdenes... por ahora", Cinema.GOLD, 3500);
				Cinema.say(player, "¡¿QUÉ?! ¡Eso es MÍO! ¡Seguridad! ¡SEGURIDAD!");
			} else {
				Cinema.actionbar(player, Text.literal("📢 Has recuperado el megáfono").formatted(Formatting.GOLD));
				Cinema.say(player, "Otra vez no...");
			}
			take.rivalry = Math.max(take.rivalry, 20);
		} else if (!has && take.hasMegaphone) {
			take.hasMegaphone = false;
		}

		if (take.hasMegaphone) {
			take.rivalry = Math.min(100, take.rivalry + 0.5f);
			if (serverTicks % 100 == 0 && player.getRandom().nextFloat() < take.rivalry / 180f) {
				counterAttack(player, take);
			}
		} else {
			take.rivalry = Math.max(0, take.rivalry - 1f);
		}
	}

	private void tickDroppedMegaphone(ServerPlayerEntity player, PlayerTake take) {
		if (take.droppedMegaphone == null) {
			return;
		}
		Entity entity = player.getServerWorld().getEntity(take.droppedMegaphone);
		if (entity == null || !entity.isAlive()) {
			take.droppedMegaphone = null;
			return;
		}
		if (--take.droppedMegaphoneTicks <= 0) {
			entity.discard();
			take.droppedMegaphone = null;
			Cinema.say(player, "Ejem. Mi megáfono. Gracias por nada.");
		} else if (take.droppedMegaphoneTicks % 10 == 0) {
			player.getServerWorld().spawnParticles(ParticleTypes.NOTE, entity.getX(), entity.getY() + 0.8, entity.getZ(), 1, 0.2, 0.2, 0.2, 1);
		}
	}

	// ---------------------------------------------------------------- the director drops it

	/** During a scene the director gets distracted and drops the megaphone near the player. */
	public void dropNear(ServerPlayerEntity player, PlayerTake take) {
		if (take.hasMegaphone || take.droppedMegaphone != null) {
			return;
		}
		ServerWorld world = player.getServerWorld();
		Vec3d spot = Props.standingSpot(world, player.getPos(), 2.5, 5, player.getRandom());
		ItemEntity item = spawnMegaphone(world, spot.add(0, 1.5, 0), Vec3d.ZERO, 10);
		take.droppedMegaphone = item.getUuid();
		take.droppedMegaphoneTicks = DROPPED_LIFETIME;
		Cinema.actionbar(player, Text.literal("📢 ¡Al director se le ha caído el megáfono! ¡Cógelo!").formatted(Formatting.GOLD, Formatting.BOLD));
	}

	private static ItemEntity spawnMegaphone(ServerWorld world, Vec3d pos, Vec3d velocity, int pickupDelay) {
		ItemEntity item = new ItemEntity(world, pos.x, pos.y, pos.z, new ItemStack(ModItems.MEGAFONO));
		item.setVelocity(velocity);
		item.setPickupDelay(pickupDelay);
		item.setGlowing(true);
		world.spawnEntity(item);
		return item;
	}

	// ---------------------------------------------------------------- your orders

	/** @return whether an order was given (the item then goes on cooldown). */
	public boolean giveOrder(ServerPlayerEntity player, PlayerTake take) {
		boolean done = switch (take.megaphoneOrder) {
			case REPLAY -> replay(player, take);
			case SLOW_MOTION -> slowMotion(player);
			case GENRE -> genre(player, take);
		};
		if (done) {
			take.rivalry = Math.min(100, take.rivalry + 18);
			take.addBoredom(-15);
			Cinema.say(player, RETORTS, player.getRandom());
			Cinema.sound(player, SoundEvents.GOAT_HORN_SOUNDS.get(0), 1.0f, 1.3f);
		}
		return done;
	}

	private boolean replay(ServerPlayerEntity player, PlayerTake take) {
		PlayerTake.Snapshot target = null;
		int index = 0;
		int wanted = Math.max(0, take.history.size() - 6);
		for (PlayerTake.Snapshot snapshot : take.history) {
			if (index++ == wanted) {
				target = snapshot;
				break;
			}
		}
		if (target == null || target.world() != player.getServerWorld().getRegistryKey()) {
			Cinema.actionbar(player, Text.literal("No hay ninguna toma que repetir").formatted(Formatting.GRAY));
			return false;
		}
		ServerWorld world = player.getServerWorld();
		world.spawnParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1, player.getZ(), 40, 0.4, 0.8, 0.4, 0.5);
		player.networkHandler.requestTeleport(target.pos().x, target.pos().y, target.pos().z, target.yaw(), target.pitch());
		player.fallDistance = 0;
		if (target.health() > player.getHealth()) {
			player.setHealth(target.health());
		}
		world.spawnParticles(ParticleTypes.REVERSE_PORTAL, target.pos().x, target.pos().y + 1, target.pos().z, 40, 0.4, 0.8, 0.4, 0.05);
		Cinema.sound(player, SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT, 1.0f, 0.7f);
		Cinema.card(player, "◀◀ REPETICIÓN", "La toma buena era la de hace 5 segundos", Cinema.CYAN, 2500);
		return true;
	}

	private boolean slowMotion(ServerPlayerEntity player) {
		MinecraftServer server = player.getServerWorld().getServer();
		if (slowMotion) {
			Cinema.actionbar(player, Text.literal("Ya estamos en cámara lenta...").formatted(Formatting.GRAY));
			return false;
		}
		server.getTickManager().setTickRate(SLOW_MOTION_TICK_RATE);
		slowMotion = true;
		slowMotionUntil = System.currentTimeMillis() + SLOW_MOTION_MS;
		for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
			Cinema.card(other, "CÁMARA LENTA", "Nooooooooooo... (pero despacio)", Cinema.WHITE, 3000);
		}
		return true;
	}

	private boolean genre(ServerPlayerEntity player, PlayerTake take) {
		Genre current = take.genre instanceof GenreScene scene ? scene.genre() : null;
		Director.INSTANCE.startScene(player, take, new GenreScene(GenreScene.randomGenre(player.getRandom(), current), true));
		return true;
	}

	// ---------------------------------------------------------------- the director fights back

	private void counterAttack(ServerPlayerEntity player, PlayerTake take) {
		Random random = player.getRandom();
		switch (random.nextInt(3)) {
			case 0 -> {
				if (snatch(player, take, player.getPos().add(0, 1.2, 0))) {
					Cinema.card(player, "¡DAME ESO!", "El director forcejea contigo por el megáfono", Cinema.RED, 2500);
					Cinema.say(player, "¡SUELTA! ¡SUÉLTALO! ¡Es mi megáfono!");
					take.rivalry -= 35;
				}
			}
			case 1 -> {
				Cinema.card(player, "¡CORTEN!", "Esa toma no sirve. Aquí mando yo.", Cinema.RED, 2500);
				Cinema.say(player, "Se acabaron tus ocurrencias. Volvemos a MI película.");
				if (take.genre instanceof GenreScene scene && scene.orderedByPlayer()) {
					take.genre.end(player, take);
					take.genre = null;
				}
				restoreTickRate(player.getServerWorld().getServer());
				Director.INSTANCE.startScene(player, take, new BudgetCutScene());
				take.rivalry -= 25;
			}
			default -> {
				Cinema.card(player, "¡SEGURIDAD!", "Recuperad mi megáfono. Por las buenas o por las malas.", Cinema.RED, 2500);
				spawnSecurity(player);
				take.rivalry -= 20;
			}
		}
		take.rivalry = Math.max(0, take.rivalry);
	}

	private void spawnSecurity(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		for (int i = 0; i < 2; i++) {
			Vec3d spot = Props.standingSpot(world, player.getPos(), 6, 10, player.getRandom());
			ZombieEntity guard = EntityType.ZOMBIE.create(world);
			if (guard == null) {
				continue;
			}
			guard.refreshPositionAndAngles(spot.x, spot.y, spot.z, 0, 0);
			guard.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
			guard.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
			for (EquipmentSlot slot : EquipmentSlot.values()) {
				guard.setEquipmentDropChance(slot, 0f);
			}
			guard.setCustomName(Text.literal("Seguridad del set").formatted(Formatting.DARK_GRAY, Formatting.BOLD));
			guard.setCustomNameVisible(true);
			guard.addCommandTag(SECURITY_TAG);
			guard.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 60, 0));
			guard.setTarget(player);
			world.spawnEntity(guard);
		}
	}

	/** Security hit you: the megaphone falls where they stand. */
	public void onHitBySecurity(ServerPlayerEntity player, PlayerTake take, Entity guard) {
		if (take.hasMegaphone && snatch(player, take, guard.getPos().add(0, 1, 0))) {
			Cinema.actionbar(player, Text.literal("📢 ¡Seguridad te ha quitado el megáfono!").formatted(Formatting.RED, Formatting.BOLD));
			Cinema.say(player, "¡Buen trabajo, chicos! Subidón de sueldo... la semana que viene.");
		}
	}

	/** Pulls the megaphone out of the inventory and throws it away from the player. */
	private boolean snatch(ServerPlayerEntity player, PlayerTake take, Vec3d from) {
		if (!removeMegaphone(player)) {
			return false;
		}
		Random random = player.getRandom();
		double angle = random.nextDouble() * Math.PI * 2;
		Vec3d velocity = new Vec3d(Math.cos(angle) * 0.5, 0.5, Math.sin(angle) * 0.5);
		ItemEntity item = spawnMegaphone(player.getServerWorld(), from, velocity, 30);
		take.hasMegaphone = false;
		take.droppedMegaphone = item.getUuid();
		take.droppedMegaphoneTicks = DROPPED_LIFETIME;
		Cinema.sound(player, SoundEvents.ENTITY_ITEM_PICKUP, 1.0f, 0.5f);
		return true;
	}

	private static int countMegaphones(ServerPlayerEntity player) {
		int count = 0;
		for (int i = 0; i < player.getInventory().size(); i++) {
			if (player.getInventory().getStack(i).isOf(ModItems.MEGAFONO)) {
				count++;
			}
		}
		return count;
	}

	private static boolean removeMegaphone(ServerPlayerEntity player) {
		for (int i = 0; i < player.getInventory().size(); i++) {
			if (player.getInventory().getStack(i).isOf(ModItems.MEGAFONO)) {
				player.getInventory().removeStack(i);
				return true;
			}
		}
		return false;
	}
}
