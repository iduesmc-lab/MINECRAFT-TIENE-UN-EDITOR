package com.iduesmc.director.scene;

import com.iduesmc.director.PlayerTake;
import com.iduesmc.director.Props;
import net.minecraft.server.network.ServerPlayerEntity;

/** A scene the director (or the player, with the megaphone) is shooting. */
public abstract class Scene {
	private static int nextId;

	/** Every prop spawned for this scene carries this tag, so it can be struck when the scene ends. */
	public final String tag = "director_scene_" + nextId++;
	protected final int duration;
	protected int age;
	protected boolean finished;

	protected Scene(int duration) {
		this.duration = duration;
	}

	public abstract String name();

	/** Genre scenes live in their own slot and can overlap director scenes. */
	public boolean isGenre() {
		return false;
	}

	public void start(ServerPlayerEntity player, PlayerTake take) {
	}

	protected void onTick(ServerPlayerEntity player, PlayerTake take) {
	}

	/** @return whether the scene keeps rolling. */
	public final boolean tick(ServerPlayerEntity player, PlayerTake take) {
		age++;
		onTick(player, take);
		return !finished && age < duration;
	}

	public void end(ServerPlayerEntity player, PlayerTake take) {
		Props.strike(player.getServerWorld().getServer(), tag);
	}

	public int secondsLeft() {
		return Math.max(0, (duration - age) / 20);
	}
}
