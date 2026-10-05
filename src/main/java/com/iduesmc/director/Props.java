package com.iduesmc.director;

import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

/**
 * Atrezzo: cardboard creepers, cameras on tripods... Built with /summon because display entities
 * are much easier to describe in NBT than through their (private) setters.
 */
public final class Props {
	/** Every prop the director spawns carries this tag. */
	public static final String PROP = "director_prop";
	/** Parts that only make sense while riding something (the cardboard on the creeper). */
	public static final String RIDER = "director_rider";

	private static final String IDENTITY = "left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f]";

	private Props() {
	}

	public static void run(ServerWorld world, Vec3d pos, String command) {
		MinecraftServer server = world.getServer();
		ServerCommandSource source = server.getCommandSource()
				.withWorld(world)
				.withPosition(pos)
				.withSilent()
				.withLevel(4);
		server.getCommandManager().executeWithPrefix(source, command);
	}

	private static String display(String block, String sceneTag, float tx, float ty, float tz, float sx, float sy, float sz, String extra) {
		return "{id:\"minecraft:block_display\",Tags:[\"" + PROP + "\",\"" + RIDER + "\",\"" + sceneTag + "\"],"
				+ "billboard:\"vertical\",block_state:{Name:\"minecraft:" + block + "\"},"
				+ "transformation:{" + IDENTITY + ",translation:[" + tx + "f," + ty + "f," + tz + "f],scale:[" + sx + "f," + sy + "f," + sz + "f]}" + extra + "}";
	}

	/** Un creeper de cartón, sostenido por un palo, que corre hacia ti. Explota con un triste «pff». */
	public static void cardboardCreeper(ServerWorld world, Vec3d pos, String sceneTag) {
		String card = display("lime_terracotta", sceneTag, -0.4f, -0.85f, 0f, 0.8f, 1.25f, 0.05f, "");
		String stick = display("stripped_oak_log", sceneTag, -0.05f, -1.75f, -0.02f, 0.1f, 1.0f, 0.1f, "");
		String label = "{id:\"minecraft:text_display\",Tags:[\"" + PROP + "\",\"" + RIDER + "\",\"" + sceneTag + "\"],"
				+ "billboard:\"vertical\",background:0,"
				+ "text:'[{\"text\":\"▀▄▀\\\\n\",\"color\":\"black\"},{\"text\":\"CREEPER\",\"color\":\"dark_green\",\"bold\":true}]',"
				+ "transformation:{" + IDENTITY + ",translation:[0f,-0.35f,0.06f],scale:[0.9f,0.9f,0.9f]}}";
		run(world, pos, "summon minecraft:creeper ~ ~ ~ {Tags:[\"" + PROP + "\",\"" + sceneTag + "\"],"
				+ "CustomName:'\"Creeper de cartón\"',ExplosionRadius:0b,Fuse:40s,"
				+ "active_effects:[{id:\"minecraft:invisibility\",duration:-1,show_particles:0b}],"
				+ "Passengers:[" + card + "," + stick + "," + label + "]}");
	}

	/** Una cámara de cine sobre un trípode, para que los zombies esperen detrás de ella. */
	public static void camera(ServerWorld world, Vec3d pos, float yaw, String sceneTag) {
		String tags = "Tags:[\"" + PROP + "\",\"" + sceneTag + "\"]";
		String rot = ",Rotation:[" + yaw + "f,0f]";
		run(world, pos, "summon minecraft:block_display ~ ~ ~ {" + tags + rot
				+ ",block_state:{Name:\"minecraft:oak_fence\"},transformation:{" + IDENTITY
				+ ",translation:[-0.1f,0f,-0.1f],scale:[0.2f,1.3f,0.2f]}}");
		run(world, pos, "summon minecraft:block_display ~ ~ ~ {" + tags + rot
				+ ",block_state:{Name:\"minecraft:observer\",Properties:{facing:\"north\"}},transformation:{" + IDENTITY
				+ ",translation:[-0.3f,1.25f,-0.4f],scale:[0.6f,0.5f,0.8f]}}");
		run(world, pos, "summon minecraft:text_display ~ ~1.95 ~ {" + tags
				+ ",billboard:\"center\",text:'{\"text\":\"● REC\",\"color\":\"red\",\"bold\":true}',background:0}");
	}

	/** Removes every prop of a scene. */
	public static void strike(MinecraftServer server, String sceneTag) {
		for (ServerWorld world : server.getWorlds()) {
			List<Entity> doomed = new ArrayList<>();
			for (Entity entity : world.iterateEntities()) {
				if (entity.getCommandTags().contains(sceneTag)) {
					doomed.add(entity);
				}
			}
			doomed.forEach(Entity::discard);
		}
	}

	/** Cardboard left behind when its creeper exploded is swept off the set. */
	public static void sweepOrphans(MinecraftServer server) {
		for (ServerWorld world : server.getWorlds()) {
			List<Entity> doomed = new ArrayList<>();
			for (Entity entity : world.iterateEntities()) {
				if (!entity.hasVehicle() && entity.getCommandTags().contains(RIDER)) {
					doomed.add(entity);
				}
			}
			doomed.forEach(Entity::discard);
		}
	}

	/** Finds a spot near {@code center} where a mob can stand, between {@code min} and {@code max} blocks away. */
	public static Vec3d standingSpot(ServerWorld world, Vec3d center, double min, double max, Random random) {
		for (int attempt = 0; attempt < 30; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double dist = min + random.nextDouble() * (max - min);
			BlockPos column = BlockPos.ofFloored(center.x + Math.cos(angle) * dist, center.y, center.z + Math.sin(angle) * dist);
			BlockPos found = standable(world, column);
			if (found != null) {
				return Vec3d.ofBottomCenter(found);
			}
		}
		return center;
	}

	/** Same, but behind the player (opposite to where they look). */
	public static Vec3d behind(ServerWorld world, Vec3d center, float yaw, double dist, double spread, Random random) {
		double rad = Math.toRadians(yaw);
		Vec3d back = new Vec3d(Math.sin(rad), 0, -Math.cos(rad)).multiply(dist);
		for (int attempt = 0; attempt < 20; attempt++) {
			double ox = (random.nextDouble() - 0.5) * 2 * spread;
			double oz = (random.nextDouble() - 0.5) * 2 * spread;
			BlockPos found = standable(world, BlockPos.ofFloored(center.add(back).add(ox, 0, oz)));
			if (found != null) {
				return Vec3d.ofBottomCenter(found);
			}
		}
		return standingSpot(world, center, 3, 6, random);
	}

	private static BlockPos standable(ServerWorld world, BlockPos column) {
		for (int dy = 0; dy <= 6; dy++) {
			for (int sign : new int[]{1, -1}) {
				BlockPos pos = column.up(dy * sign);
				if (world.getBlockState(pos).isAir()
						&& world.getBlockState(pos.up()).isAir()
						&& world.getBlockState(pos.down()).isSideSolidFullSquare(world, pos.down(), Direction.UP)) {
					return pos;
				}
			}
		}
		return null;
	}
}
