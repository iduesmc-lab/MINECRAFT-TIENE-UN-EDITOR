package com.iduesmc.director.item;

import com.iduesmc.director.Director;
import com.iduesmc.director.PlayerTake;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/**
 * El megáfono robado. Clic derecho: dar la orden seleccionada. Agachado + clic derecho: cambiar de orden.
 */
public class MegaphoneItem extends Item {
	public MegaphoneItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient() || !(user instanceof ServerPlayerEntity player)) {
			return TypedActionResult.success(stack, world.isClient());
		}
		PlayerTake take = Director.INSTANCE.take(player);
		if (player.isSneaking()) {
			take.megaphoneOrder = take.megaphoneOrder.next();
			player.sendMessage(Text.literal("🎬 Orden: ").formatted(Formatting.GRAY)
					.append(Text.literal(take.megaphoneOrder.label).formatted(Formatting.GOLD, Formatting.BOLD)), true);
			return TypedActionResult.success(stack);
		}
		if (Director.INSTANCE.megaphone().giveOrder(player, take)) {
			player.getItemCooldownManager().set(this, 200);
		}
		return TypedActionResult.success(stack);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.literal("Propiedad del Director. NO TOCAR.").formatted(Formatting.RED, Formatting.ITALIC));
		tooltip.add(Text.literal("Clic derecho: ¡dar la orden!").formatted(Formatting.GRAY));
		tooltip.add(Text.literal("Agachado + clic derecho: cambiar de orden").formatted(Formatting.GRAY));
		tooltip.add(Text.literal("Repetición · Cámara lenta · Cambio de género").formatted(Formatting.DARK_AQUA));
	}
}
