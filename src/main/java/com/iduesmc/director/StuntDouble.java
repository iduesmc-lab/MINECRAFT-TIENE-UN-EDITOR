package com.iduesmc.director;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * «DOBLE DE ACCIÓN». When you would die, a villager wearing your face takes the hit
 * while you are launched out of frame.
 */
public final class StuntDouble {
	public static final int COOLDOWN_TICKS = 20 * 60 * 4;
	private static final List<String> LINES = List.of(
			"¡Perfecto! El doble se lo ha comido todo. Tú, fuera de plano. ¡YA!",
			"Nadie notará la diferencia. Bueno, la nariz quizá.",
			"Ese aldeano tenía familia. Bueno, tenía un contrato. Es casi lo mismo.",
			"Seguro de rodaje: usado. Próximo doble disponible en 4 minutos.");

	private StuntDouble() {
	}

	/** @return {@code true} if the player really dies, {@code false} if the stunt double took the hit. */
	public static boolean allowDeath(ServerPlayerEntity player, PlayerTake take, DamageSource source) {
		if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY) || take.stuntCooldown > 0 || player.isSpectator()) {
			return true;
		}
		ServerWorld world = player.getServerWorld();
		VillagerEntity stunt = EntityType.VILLAGER.create(world);
		if (stunt == null) {
			return true;
		}

		ItemStack face = new ItemStack(Items.PLAYER_HEAD);
		face.set(DataComponentTypes.PROFILE, new ProfileComponent(player.getGameProfile()));
		stunt.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), 0);
		stunt.equipStack(EquipmentSlot.HEAD, face);
		stunt.equipStack(EquipmentSlot.MAINHAND, player.getMainHandStack().copy());
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			stunt.setEquipmentDropChance(slot, 0f);
		}
		stunt.setCustomName(Text.literal("Doble de acción de " + player.getName().getString()).formatted(Formatting.YELLOW));
		stunt.setCustomNameVisible(true);
		world.spawnEntity(stunt);
		stunt.damage(source, 1000f);
		if (stunt.isAlive()) {
			stunt.kill();
		}

		// The real actor survives...
		player.setHealth(Math.min(player.getMaxHealth(), 8f));
		player.extinguish();
		player.fallDistance = 0;
		player.removeStatusEffect(StatusEffects.POISON);
		player.removeStatusEffect(StatusEffects.WITHER);
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 20 * 10, 0));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20 * 5, 4));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 20 * 8, 0));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 20 * 4, 0, false, false));

		// ...and is yanked out of frame.
		Vec3d away;
		Vec3d from = source.getPosition();
		if (from != null && from.squaredDistanceTo(player.getPos()) > 0.01) {
			away = player.getPos().subtract(from);
		} else {
			double angle = player.getRandom().nextDouble() * Math.PI * 2;
			away = new Vec3d(Math.cos(angle), 0, Math.sin(angle));
		}
		away = new Vec3d(away.x, 0, away.z).normalize();
		player.setVelocity(away.x * 2.2, 1.3, away.z * 2.2);
		player.velocityModified = true;

		for (MobEntity mob : world.getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(24), m -> m.getTarget() == player)) {
			mob.setTarget(null);
		}

		world.spawnParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1, player.getZ(), 30, 0.5, 0.5, 0.5, 0.1);
		Cinema.sound(player, SoundEvents.ENTITY_VILLAGER_DEATH, 1.0f, 1.0f);
		Cinema.sound(player, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 0.8f);
		Cinema.card(player, "¡DOBLE DE ACCIÓN!", "El aldeano cobró por esta escena", Cinema.RED, 3000);
		Cinema.say(player, LINES, player.getRandom());

		take.stuntCooldown = COOLDOWN_TICKS;
		take.addBoredom(-40);
		return false;
	}
}
