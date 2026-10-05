package com.iduesmc.director;

import com.iduesmc.director.scene.Scene;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/** Everything the director remembers about one player's "toma". */
public class PlayerTake {
	public static final float MAX_BOREDOM = 100f;

	public final UUID playerId;
	public final ServerBossBar bar = new ServerBossBar(Text.literal("🎬 Interés del público"), BossBar.Color.GREEN, BossBar.Style.NOTCHED_10);

	/** 0 = the audience is glued to the screen, 100 = the director cuts the scene. */
	public float boredom;
	/** Ticks before the director may interfere again. Starts with some grace after joining. */
	public int directorCooldown = 20 * 60;
	/** Server tick of every recent stone block mined (the director hates "picar piedra"). */
	public final Deque<Long> stoneMined = new ArrayDeque<>();
	@Nullable
	public Vec3d lastSecondPos;

	/** One snapshot per second, newest last, for the megaphone's REPETICIÓN. */
	public final Deque<Snapshot> history = new ArrayDeque<>();

	public int takeNumber = 1;
	/** Ticks until the "TOMA N" card is shown (after joining or respawning). */
	public int joinCardDelay;
	public int stuntCooldown;
	public int sponsorCooldown;

	/** Director scenes (budget cut, plot twist...) and genre scenes run at the same time. */
	@Nullable
	public Scene scene;
	@Nullable
	public Scene genre;

	// --- El megáfono ---
	public boolean hasMegaphone;
	public boolean everStoleMegaphone;
	public MegaphoneOrder megaphoneOrder = MegaphoneOrder.REPLAY;
	/** How annoyed the director is that you have his megaphone. 0..100. */
	public float rivalry;
	@Nullable
	public UUID droppedMegaphone;
	public int droppedMegaphoneTicks;

	public PlayerTake(UUID playerId) {
		this.playerId = playerId;
	}

	public void addBoredom(float amount) {
		boredom = Math.max(0, Math.min(MAX_BOREDOM, boredom + amount));
	}

	public void refreshBar() {
		float interest = 1f - boredom / MAX_BOREDOM;
		bar.setPercent(interest);
		BossBar.Color color = interest > 0.6f ? BossBar.Color.GREEN : interest > 0.3f ? BossBar.Color.YELLOW : BossBar.Color.RED;
		bar.setColor(color);
		Text name = Text.literal("🎬 Interés del público").formatted(Formatting.BOLD);
		if (hasMegaphone) {
			name = Text.literal("🎬 Interés del público  ").formatted(Formatting.BOLD)
					.append(Text.literal("| Furia del director: " + Math.round(rivalry) + "%").formatted(Formatting.RED));
		}
		bar.setName(name);
	}

	public record Snapshot(RegistryKey<World> world, Vec3d pos, float yaw, float pitch, float health) {
	}
}
