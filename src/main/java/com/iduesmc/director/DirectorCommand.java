package com.iduesmc.director;

import com.iduesmc.director.item.ModItems;
import com.iduesmc.director.scene.BudgetCutScene;
import com.iduesmc.director.scene.Genre;
import com.iduesmc.director.scene.GenreScene;
import com.iduesmc.director.scene.PlotTwistScene;
import com.iduesmc.director.scene.Scene;
import com.iduesmc.director.scene.SponsorScene;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * /director — para probar el mod sin esperar a aburrirte.
 */
public final class DirectorCommand {
	private DirectorCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			LiteralArgumentBuilder<ServerCommandSource> escena = CommandManager.literal("escena")
					.then(scene("presupuesto", BudgetCutScene::new))
					.then(scene("patrocinador", SponsorScene::new))
					.then(scene("villano", () -> new PlotTwistScene(PlotTwistScene.Twist.VILLAIN)))
					.then(scene("gemelo", () -> new PlotTwistScene(PlotTwistScene.Twist.EVIL_TWIN)))
					.then(scene("persecucion", () -> new PlotTwistScene(PlotTwistScene.Twist.CHASE)));
			LiteralArgumentBuilder<ServerCommandSource> genero = CommandManager.literal("genero");
			for (Genre genre : Genre.values()) {
				genero.then(scene(genre.name().toLowerCase(Locale.ROOT), () -> new GenreScene(genre, false)));
			}
			escena.then(genero);

			dispatcher.register(CommandManager.literal("director")
					.requires(source -> source.hasPermissionLevel(2))
					.then(escena)
					.then(CommandManager.literal("estado").executes(DirectorCommand::status))
					.then(CommandManager.literal("corten").executes(context -> {
						ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
						Director.INSTANCE.cut(player, Director.INSTANCE.take(player));
						context.getSource().sendFeedback(() -> Text.literal("¡Corten! Todas las escenas terminadas."), false);
						return 1;
					}))
					.then(CommandManager.literal("megafono").executes(context -> {
						ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
						ItemStack megaphone = new ItemStack(ModItems.MEGAFONO);
						if (!player.giveItemStack(megaphone)) {
							player.dropItem(megaphone, false);
						}
						return 1;
					}))
					.then(CommandManager.literal("doble").executes(context -> {
						ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
						Director.INSTANCE.take(player).stuntCooldown = 0;
						context.getSource().sendFeedback(() -> Text.literal("El doble de acción está listo."), false);
						return 1;
					}))
					.then(CommandManager.literal("aburrimiento")
							.then(CommandManager.argument("valor", IntegerArgumentType.integer(0, 100)).executes(context -> {
								ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
								PlayerTake take = Director.INSTANCE.take(player);
								take.boredom = IntegerArgumentType.getInteger(context, "valor");
								take.directorCooldown = 0;
								take.refreshBar();
								return 1;
							}))));
		});
	}

	private static LiteralArgumentBuilder<ServerCommandSource> scene(String name, Supplier<Scene> factory) {
		return CommandManager.literal(name).executes(context -> {
			ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
			Director.INSTANCE.startScene(player, Director.INSTANCE.take(player), factory.get());
			return 1;
		});
	}

	private static int status(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
		PlayerTake take = Director.INSTANCE.take(player);
		String text = "🎬 Toma " + take.takeNumber
				+ " | Aburrimiento " + Math.round(take.boredom) + "%"
				+ " | Escena: " + (take.scene == null ? "—" : take.scene.name())
				+ " | Género: " + (take.genre == null ? "—" : take.genre.name())
				+ " | Megáfono: " + (take.hasMegaphone ? "TUYO (furia " + Math.round(take.rivalry) + "%)" : "del director")
				+ " | Doble listo en: " + take.stuntCooldown / 20 + "s";
		context.getSource().sendFeedback(() -> Text.literal(text), false);
		return 1;
	}
}
