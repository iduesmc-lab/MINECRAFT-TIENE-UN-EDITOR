package com.iduesmc.director.scene;

import com.iduesmc.director.Cinema;
import com.iduesmc.director.PlayerTake;
import com.iduesmc.director.Props;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * «CORTE. FALTA PRESUPUESTO». Everything around the player turns into cardboard (only on their screen:
 * the world is untouched) and a cardboard creeper on a stick runs at them.
 */
public class BudgetCutScene extends Scene {
	private static final int RADIUS = 8;
	private static final List<String> LINES = List.of(
			"¿Otra vez picando piedra? El público se ha dormido. ¡Recortamos!",
			"Producción dice que los bloques de verdad eran muy caros.",
			"El creeper de verdad cobraba demasiado. Te presento a su sustituto.",
			"Si alguien pregunta, esto es «estilo artístico».");

	private final Set<BlockPos> faked = new HashSet<>();
	@Nullable
	private RegistryKey<World> fakedWorld;

	public BudgetCutScene() {
		super(20 * 35);
	}

	@Override
	public String name() {
		return "Falta presupuesto";
	}

	@Override
	public void start(ServerPlayerEntity player, PlayerTake take) {
		Cinema.card(player, "CORTE.", "FALTA PRESUPUESTO", Cinema.CARDBOARD, 3500);
		Cinema.say(player, LINES, player.getRandom());
		Cinema.sound(player, SoundEvents.BLOCK_SCAFFOLDING_BREAK, 1.0f, 0.6f);
		ServerWorld world = player.getServerWorld();
		Props.cardboardCreeper(world, Props.standingSpot(world, player.getPos(), 9, 13, player.getRandom()), tag);
		refresh(player);
	}

	@Override
	protected void onTick(ServerPlayerEntity player, PlayerTake take) {
		if (age % 30 == 0) {
			refresh(player);
		}
		if (age % 40 == 0) {
			Cinema.actionbar(player, Text.literal("💸 Presupuesto restante: 0,00 €").formatted(Formatting.GOLD));
		}
		if (age % 70 == 0) {
			Cinema.sound(player, SoundEvents.BLOCK_BAMBOO_WOOD_HIT, 0.6f, 0.7f + player.getRandom().nextFloat() * 0.4f);
		}
	}

	@Override
	public void end(ServerPlayerEntity player, PlayerTake take) {
		restoreAll(player);
		super.end(player, take);
		if (!player.isRemoved()) {
			Cinema.card(player, "PRESUPUESTO RECUPERADO", "Un inversor anónimo ha pagado los bloques de verdad", Cinema.GOLD, 2500);
		}
	}

	private void refresh(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		if (fakedWorld != null && fakedWorld != world.getRegistryKey()) {
			faked.clear();
		}
		fakedWorld = world.getRegistryKey();

		BlockPos center = player.getBlockPos();
		Set<BlockPos> wanted = new HashSet<>();
		for (BlockPos mutable : BlockPos.iterate(center.add(-RADIUS, -RADIUS, -RADIUS), center.add(RADIUS, RADIUS, RADIUS))) {
			if (mutable.getSquaredDistance(center) > RADIUS * RADIUS) {
				continue;
			}
			BlockState real = world.getBlockState(mutable);
			BlockState cardboard = cardboardFor(world, mutable, real);
			if (cardboard == null || !exposed(world, mutable)) {
				continue;
			}
			BlockPos pos = mutable.toImmutable();
			wanted.add(pos);
			if (!faked.contains(pos)) {
				player.networkHandler.sendPacket(new BlockUpdateS2CPacket(pos, cardboard));
			}
		}
		for (BlockPos pos : faked) {
			if (!wanted.contains(pos)) {
				player.networkHandler.sendPacket(new BlockUpdateS2CPacket(world, pos));
			}
		}
		faked.clear();
		faked.addAll(wanted);
	}

	private void restoreAll(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		if (world.getRegistryKey() == fakedWorld) {
			for (BlockPos pos : faked) {
				player.networkHandler.sendPacket(new BlockUpdateS2CPacket(world, pos));
			}
		}
		faked.clear();
	}

	/** Picks a cardboard look-alike that is mined with the same tool, so mining still feels right. */
	@Nullable
	private static BlockState cardboardFor(ServerWorld world, BlockPos pos, BlockState real) {
		if (real.isAir() || real.hasBlockEntity() || !real.isOpaqueFullCube(world, pos)) {
			return null;
		}
		// Ores stay real: the loot is the only thing with budget.
		if (Registries.BLOCK.getId(real.getBlock()).getPath().endsWith("_ore") || real.isOf(Blocks.BEDROCK)) {
			return null;
		}
		if (real.isIn(BlockTags.SHOVEL_MINEABLE)) {
			return Blocks.BROWN_CONCRETE_POWDER.getDefaultState();
		}
		if (real.isIn(BlockTags.AXE_MINEABLE)) {
			return Blocks.BIRCH_PLANKS.getDefaultState();
		}
		return Math.floorMod(pos.hashCode(), 9) == 0 ? Blocks.BROWN_TERRACOTTA.getDefaultState() : Blocks.TERRACOTTA.getDefaultState();
	}

	private static boolean exposed(ServerWorld world, BlockPos pos) {
		for (Direction direction : Direction.values()) {
			BlockPos neighbor = pos.offset(direction);
			if (!world.getBlockState(neighbor).isOpaqueFullCube(world, neighbor)) {
				return true;
			}
		}
		return false;
	}
}
