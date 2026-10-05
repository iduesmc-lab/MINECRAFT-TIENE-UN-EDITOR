package com.iduesmc.director.scene;

import com.iduesmc.director.Cinema;
import com.iduesmc.director.PlayerTake;
import com.iduesmc.director.Props;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * «EL PATROCINADOR EXIGE UNA MENCIÓN». You found diamonds: now you have to advertise a horrible shovel
 * in chat while the zombies wait behind the cameras.
 */
public class SponsorScene extends Scene {
	private static final List<String> SLOGANS = List.of(
			"Pala Horrible™: cava peor, presume más.",
			"Pala Horrible™: la única pala que te pide perdón.",
			"Pala Horrible™: ahora con un 3% menos de astillas.",
			"Pala Horrible™: porque una pala normal no te haría sufrir.",
			"Pala Horrible™: recomendada por 0 de cada 10 mineros.");
	public static final String EXTRA_TAG = "director_extra";

	private final List<UUID> frozen = new ArrayList<>();
	private final List<UUID> extras = new ArrayList<>();
	private String slogan = SLOGANS.get(0);
	private boolean mentioned;

	public SponsorScene() {
		super(20 * 40);
	}

	@Override
	public String name() {
		return "Mención del patrocinador";
	}

	@Override
	public void start(ServerPlayerEntity player, PlayerTake take) {
		ServerWorld world = player.getServerWorld();
		slogan = SLOGANS.get(player.getRandom().nextInt(SLOGANS.size()));
		Cinema.card(player, "EL PATROCINADOR EXIGE UNA MENCIÓN", "Escribe en el chat: «Pala Horrible»", Cinema.GOLD, 4000);
		Cinema.say(player, "¡Diamantes! Justo a tiempo para la pausa publicitaria.");
		Cinema.say(player, "Lee esto con ENTUSIASMO en el chat: «" + slogan + "»");
		Cinema.sound(player, SoundEvents.ENTITY_VILLAGER_CELEBRATE, 1.0f, 1.0f);

		ItemStack shovel = new ItemStack(Items.WOODEN_SHOVEL);
		shovel.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Pala Horrible™").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
		shovel.set(DataComponentTypes.LORE, new LoreComponent(List.of(
				Text.literal(slogan).formatted(Formatting.GRAY, Formatting.ITALIC),
				Text.literal("Producto patrocinado. No apto para cavar.").formatted(Formatting.DARK_GRAY))));
		shovel.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
		if (!player.giveItemStack(shovel)) {
			player.dropItem(shovel, false);
		}

		// Everyone hostile goes on a break...
		for (HostileEntity mob : world.getEntitiesByClass(HostileEntity.class, player.getBoundingBox().expand(24), m -> !m.isAiDisabled())) {
			mob.setAiDisabled(true);
			frozen.add(mob.getUuid());
		}
		// ...and a few extras wait behind the cameras.
		float yawToPlayer;
		for (int i = 0; i < 3; i++) {
			Vec3d spot = Props.behind(world, player.getPos(), player.getYaw(), 8, 2.5, player.getRandom());
			ZombieEntity zombie = EntityType.ZOMBIE.create(world);
			if (zombie == null) {
				continue;
			}
			yawToPlayer = yawTowards(spot, player.getPos());
			zombie.refreshPositionAndAngles(spot.x, spot.y, spot.z, yawToPlayer, 0);
			zombie.setHeadYaw(yawToPlayer);
			zombie.setBodyYaw(yawToPlayer);
			zombie.setAiDisabled(true);
			zombie.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
			zombie.setEquipmentDropChance(EquipmentSlot.HEAD, 0f);
			zombie.setCustomName(Text.literal("Figurante (en pausa)").formatted(Formatting.GRAY));
			zombie.setCustomNameVisible(true);
			zombie.addCommandTag(EXTRA_TAG);
			world.spawnEntity(zombie);
			extras.add(zombie.getUuid());
		}
		Vec3d cameraSpot = Props.behind(world, player.getPos(), player.getYaw(), 4, 0.5, player.getRandom());
		Props.camera(world, cameraSpot, yawTowards(cameraSpot, player.getPos()), tag);
	}

	@Override
	protected void onTick(ServerPlayerEntity player, PlayerTake take) {
		if (age % 20 == 0) {
			Cinema.actionbar(player, Text.literal("📢 Menciona al patrocinador en el chat: ").formatted(Formatting.GOLD)
					.append(Text.literal(secondsLeft() + "s").formatted(Formatting.RED, Formatting.BOLD)));
		}
		// The extras wait patiently. They must not burn in the sun while on a break.
		if (age % 20 == 0) {
			for (UUID id : extras) {
				Entity entity = player.getServerWorld().getEntity(id);
				if (entity != null) {
					entity.extinguish();
				}
			}
		}
	}

	/** Called from the chat event. */
	public void onChat(ServerPlayerEntity player, String message) {
		if (!finished && normalize(message).contains("pala horrible")) {
			mentioned = true;
			finished = true;
		}
	}

	@Override
	public void end(ServerPlayerEntity player, PlayerTake take) {
		ServerWorld world = player.getServerWorld();
		super.end(player, take);
		if (!player.isRemoved()) {
			if (mentioned) {
				Cinema.card(player, "¡GRACIAS, PATROCINADOR!", "Pago recibido: 1 diamante y algo de experiencia", Cinema.GOLD, 3000);
				Cinema.say(player, "¡Qué naturalidad! Nadie notará que es un anuncio. ¡Y... ACCIÓN!");
				ItemStack bonus = new ItemStack(Items.DIAMOND);
				if (!player.giveItemStack(bonus)) {
					player.dropItem(bonus, false);
				}
				player.addExperience(30);
				take.addBoredom(-30);
				Cinema.sound(player, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
			} else {
				Cinema.card(player, "EL PATROCINADOR SE RETIRA", "Incumplimiento de contrato: -1 diamante", Cinema.RED, 3000);
				Cinema.say(player, "¡Perdimos al patrocinador! Que los figurantes salgan... ¡con GANAS!");
				removeOneDiamond(player);
				Cinema.sound(player, SoundEvents.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
			}
		}
		for (UUID id : frozen) {
			if (world.getEntity(id) instanceof MobEntity mob) {
				mob.setAiDisabled(false);
			}
		}
		for (UUID id : extras) {
			if (world.getEntity(id) instanceof MobEntity mob) {
				mob.setAiDisabled(false);
				mob.setCustomName(Text.literal("Figurante").formatted(Formatting.GRAY));
				if (!mentioned) {
					mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 30, 0));
				}
			}
		}
	}

	private static void removeOneDiamond(ServerPlayerEntity player) {
		for (int i = 0; i < player.getInventory().size(); i++) {
			ItemStack stack = player.getInventory().getStack(i);
			if (stack.isOf(Items.DIAMOND)) {
				stack.decrement(1);
				return;
			}
		}
	}

	private static float yawTowards(Vec3d from, Vec3d to) {
		double dx = to.x - from.x;
		double dz = to.z - from.z;
		return (float) (MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90f;
	}

	private static String normalize(String text) {
		String plain = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return plain.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ");
	}
}
