package com.iduesmc.director.scene;

import com.iduesmc.director.Cinema;
import com.iduesmc.director.PlayerTake;
import com.iduesmc.director.Props;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

/** «GIRO DE GUION». The director decides the plot needs a twist, right now. */
public class PlotTwistScene extends Scene {
	public enum Twist {
		VILLAIN, EVIL_TWIN, CHASE
	}

	private final Twist twist;

	public PlotTwistScene(Twist twist) {
		super(20 * 20);
		this.twist = twist;
	}

	@Override
	public String name() {
		return "Giro de guion: " + twist.name().toLowerCase();
	}

	@Override
	public void start(ServerPlayerEntity player, PlayerTake take) {
		ServerWorld world = player.getServerWorld();
		switch (twist) {
			case VILLAIN -> {
				Cinema.card(player, "GIRO DE GUION", "Entra en escena... ¡el villano principal!", Cinema.RED, 3500);
				Cinema.say(player, "Necesito un antagonista. Tú, el del hacha. Sí, tú. ¡Entras!");
				Vec3d spot = Props.standingSpot(world, player.getPos(), 10, 14, player.getRandom());
				MobEntity villain = EntityType.VINDICATOR.create(world);
				if (villain != null) {
					villain.refreshPositionAndAngles(spot.x, spot.y, spot.z, 0, 0);
					villain.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
					villain.setCustomName(Text.literal("Villano Principal").formatted(Formatting.DARK_RED, Formatting.BOLD));
					villain.setCustomNameVisible(true);
					villain.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 20 * 60, 0));
					villain.setTarget(player);
					world.spawnEntity(villain);
					dramaticLightning(world, spot);
				}
			}
			case EVIL_TWIN -> {
				String name = player.getName().getString();
				Cinema.card(player, "GIRO DE GUION", "Tienes un gemelo malvado. Siempre lo tuviste.", Cinema.RED, 3500);
				Cinema.say(player, "El público no lo vio venir. Yo tampoco, se me acaba de ocurrir.");
				Vec3d spot = Props.standingSpot(world, player.getPos(), 7, 11, player.getRandom());
				MobEntity twin = EntityType.ZOMBIE.create(world);
				if (twin != null) {
					ItemStack head = new ItemStack(Items.PLAYER_HEAD);
					head.set(DataComponentTypes.PROFILE, new ProfileComponent(player.getGameProfile()));
					twin.refreshPositionAndAngles(spot.x, spot.y, spot.z, 0, 0);
					twin.equipStack(EquipmentSlot.HEAD, head);
					twin.equipStack(EquipmentSlot.MAINHAND, player.getMainHandStack().copy());
					for (EquipmentSlot slot : EquipmentSlot.values()) {
						twin.setEquipmentDropChance(slot, 0f);
					}
					twin.setCustomName(Text.literal("Gemelo malvado de " + name).formatted(Formatting.DARK_PURPLE));
					twin.setCustomNameVisible(true);
					twin.setTarget(player);
					world.spawnEntity(twin);
					dramaticLightning(world, spot);
				}
			}
			case CHASE -> {
				Cinema.card(player, "ESCENA DE PERSECUCIÓN", "¡CORRE! (el presupuesto de dobles se ha agotado)", Cinema.RED, 3500);
				Cinema.say(player, "Quiero ver sudor. Quiero ver pánico. ¡Que suene la música épica!");
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 20, 1));
				for (int i = 0; i < 4; i++) {
					Vec3d spot = Props.behind(world, player.getPos(), player.getYaw(), 14, 3, player.getRandom());
					MobEntity chaser = EntityType.ZOMBIE.create(world);
					if (chaser != null) {
						chaser.refreshPositionAndAngles(spot.x, spot.y, spot.z, 0, 0);
						chaser.setCustomName(Text.literal("Perseguidor #" + (i + 1)).formatted(Formatting.GRAY));
						chaser.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 20, 0));
						chaser.setTarget(player);
						world.spawnEntity(chaser);
					}
				}
				Cinema.sound(player, SoundEvents.EVENT_RAID_HORN, 1.0f, 1.2f);
			}
		}
	}

	private static void dramaticLightning(ServerWorld world, Vec3d pos) {
		LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(world);
		if (lightning != null) {
			lightning.refreshPositionAfterTeleport(pos);
			lightning.setCosmetic(true);
			world.spawnEntity(lightning);
		}
	}
}
